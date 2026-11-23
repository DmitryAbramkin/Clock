package com.olivka.clock.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.olivka.clock.MainActivity
import com.olivka.clock.R
import com.olivka.clock.StrictClockApplication
import com.olivka.clock.utils.VibrationUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AlarmService : Service() {
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var quietNotification = false

    private val prefs by lazy {
        getSharedPreferences("strict_clock_prefs", MODE_PRIVATE)
    }

    companion object {
        private const val CHANNEL_ID = "alarm_channel_v3"
        /**
         * Same alarm notification without heads-up. Used while the app UI is
         * visible: the full-screen intent (and its high-importance channel)
         * is only needed to bring the UI up from the background.
         */
        private const val QUIET_CHANNEL_ID = "alarm_channel_v4_quiet"
        private const val NOTIFICATION_ID = 1001

        @Volatile
        private var runningInstance: AlarmService? = null

        /** Re-posts the ringing notification without heads-up once the UI is up. */
        fun onAppVisible() {
            runningInstance?.useQuietNotification()
        }

        private val _ringing = MutableStateFlow(false)
        val ringing: StateFlow<Boolean> = _ringing.asStateFlow()
        val isRinging: Boolean get() = _ringing.value

        var currentAlarmId = -1
        var currentSoundUri = ""
        var currentVibrationEnabled = true
        var currentIsTimer = false
        var currentTimerId = -1
        var currentTimerLabel = ""
    }

    override fun onCreate() {
        super.onCreate()
        runningInstance = this
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.alarm_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                setSound(null, null)
                enableVibration(false)
                setShowBadge(false)
            }
            manager?.createNotificationChannel(channel)
            val quietChannel = NotificationChannel(
                QUIET_CHANNEL_ID,
                getString(R.string.alarm_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                setSound(null, null)
                enableVibration(false)
                setShowBadge(false)
            }
            manager?.createNotificationChannel(quietChannel)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            // START_STICKY restart without an alarm. Ringing a "ghost" alarm here
            // would keep the phone awake with no UI and no way to dismiss it.
            stopSelf(startId)
            return START_NOT_STICKY
        }

        val alarmId = intent.getIntExtra("ALARM_ID", -1)
        val requestedSoundUri = intent.getStringExtra("SOUND_URI") ?: ""
        val isTimer = intent.getBooleanExtra("IS_TIMER", false)
        val vibrationFollowsGlobal = intent.getBooleanExtra(
            "VIBRATION_FOLLOWS_GLOBAL",
            false
        )
        val vibrationEnabled = if (vibrationFollowsGlobal) {
            prefs.getBoolean(
                if (isTimer) {
                    VibrationUtils.PREF_TIMER_DEFAULT
                } else {
                    VibrationUtils.PREF_ALARM_DEFAULT
                },
                true
            )
        } else {
            intent.getBooleanExtra("VIBRATION_ENABLED", true)
        }
        val timerId = intent.getIntExtra("TIMER_ID", -1)
        val timerLabel = intent.getStringExtra("TIMER_LABEL") ?: ""
        val soundUriStr = when {
            // A timer can carry its own sound. Empty means it was created
            // with the global timer sound and should follow it.
            isTimer && requestedSoundUri == "Silent" -> "Silent"
            isTimer && requestedSoundUri.isNotEmpty() -> requestedSoundUri
            isTimer -> prefs.getString("timer_sound", "") ?: ""
            // "Default" on an alarm follows the app-wide alarm sound.
            requestedSoundUri.isEmpty() ->
                prefs.getString("default_alarm_sound", "") ?: ""
            else -> requestedSoundUri
        }

        // A ringing alarm already has the user's attention; a simultaneous
        // timer must not overwrite it and lose the alarm state. Do not call
        // stopSelf(startId) here: that would stop the service that is already
        // ringing, because this is the most recent start id.
        if (isTimer && _ringing.value) {
            return START_STICKY
        }

        _ringing.value = true
        currentAlarmId = alarmId
        currentSoundUri = soundUriStr
        currentVibrationEnabled = vibrationEnabled
        currentIsTimer = isTimer
        currentTimerId = timerId
        currentTimerLabel = timerLabel

        // A visible app already shows the wake-up UI via the ringing state.
        // Its notification must stay in the shade without a heads-up there;
        // the high-importance channel and the full-screen intent are only
        // needed to bring the UI up from the background.
        val headsUp = !MainActivity.isVisible
        quietNotification = !headsUp
        val notification = buildAlarmNotification(
            isTimer,
            alarmId,
            soundUriStr,
            vibrationEnabled,
            timerId,
            timerLabel,
            headsUp
        )
        val notificationManager = getSystemService(NotificationManager::class.java)

        // Post the alarm notification explicitly before becoming foreground.
        // If the app is in the system/OEM "background restricted" state,
        // startForeground() is silently ignored on Android 12+ (ActiveServices
        // logs "not allowed due to bg restriction"), and the notification with
        // its full-screen intent would never be posted. notify() keeps the
        // alarm visible in that case.
        try {
            notificationManager?.notify(NOTIFICATION_ID, notification)
        } catch (_: Exception) {
            // POST_NOTIFICATIONS revoked or notification channel disabled.
        }

        try {
            startForeground(NOTIFICATION_ID, notification)
        } catch (_: Exception) {
            // ForegroundServiceStartNotAllowedException and OEM variants.
        }

        if (headsUp && needsDirectWakeUp(notificationManager)) {
            launchWakeUpActivity(alarmId, soundUriStr, vibrationEnabled, isTimer, timerId, timerLabel)
        }

        if (!isTimer) {
            scheduleNextOccurrence(alarmId)
        }
        ensureAlarmVolume()

        playRingtone(soundUriStr)
        if (vibrationEnabled) {
            startVibration()
        }

        return START_STICKY
    }

    private fun buildAlarmNotification(
        isTimer: Boolean,
        alarmId: Int,
        soundUriStr: String,
        vibrationEnabled: Boolean,
        timerId: Int,
        timerLabel: String,
        headsUp: Boolean
    ): Notification {
        val fullScreenIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("IS_WAKE_UP", true)
            putExtra("IS_TIMER", isTimer)
            putExtra("ALARM_ID", alarmId)
            putExtra("SOUND_URI", soundUriStr)
            putExtra("VIBRATION_ENABLED", vibrationEnabled)
            putExtra("TIMER_ID", timerId)
            putExtra("TIMER_LABEL", timerLabel)
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            alarmId,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(
            this,
            if (headsUp) CHANNEL_ID else QUIET_CHANNEL_ID
        )
            .setContentTitle(
                if (isTimer && timerLabel.isNotBlank()) {
                    timerLabel
                } else {
                    getString(
                        if (isTimer) {
                            R.string.timer_finished_notification_title
                        } else {
                            R.string.alarm_notification_title
                        }
                    )
                }
            )
            .setContentText(
                getString(
                    if (isTimer) {
                        R.string.timer_finished_notification_text
                    } else {
                        R.string.alarm_notification_text
                    }
                )
            )
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setPriority(
                if (headsUp) {
                    NotificationCompat.PRIORITY_MAX
                } else {
                    NotificationCompat.PRIORITY_DEFAULT
                }
            )
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(fullScreenPendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
        if (headsUp) {
            builder.setFullScreenIntent(fullScreenPendingIntent, true)
        }
        return builder.build()
    }

    /**
     * The UI became visible while the alarm rings. Replace the high-importance
     * notification with the quiet one so a heads-up does not cover the wake-up
     * screen; the notification itself stays in the shade.
     */
    private fun useQuietNotification() {
        if (!_ringing.value || quietNotification) return
        quietNotification = true
        val notification = buildAlarmNotification(
            currentIsTimer,
            currentAlarmId,
            currentSoundUri,
            currentVibrationEnabled,
            currentTimerId,
            currentTimerLabel,
            headsUp = false
        )
        try {
            startForeground(NOTIFICATION_ID, notification)
        } catch (_: Exception) {
            // The service may not have become foreground at all.
        }
        try {
            getSystemService(NotificationManager::class.java)
                ?.notify(NOTIFICATION_ID, notification)
        } catch (_: Exception) {
            // POST_NOTIFICATIONS revoked or notification channel disabled.
        }
    }

    /**
     * On Android 14+ the user can revoke the full-screen intent permission. If
     * notifications are disabled there is nothing for the FSI to ride on either.
     * In both cases the wake-up activity is started directly; exact alarms grant
     * a background activity start allowlist, and SYSTEM_ALERT_WINDOW is a second
     * exemption on most devices.
     */
    private fun needsDirectWakeUp(notificationManager: NotificationManager?): Boolean {
        if (notificationManager == null) return true
        if (!notificationManager.areNotificationsEnabled()) return true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            return !notificationManager.canUseFullScreenIntent()
        }
        return false
    }

    private fun launchWakeUpActivity(
        alarmId: Int,
        soundUriStr: String,
        vibrationEnabled: Boolean,
        isTimer: Boolean,
        timerId: Int,
        timerLabel: String
    ) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("IS_WAKE_UP", true)
            putExtra("IS_TIMER", isTimer)
            putExtra("ALARM_ID", alarmId)
            putExtra("SOUND_URI", soundUriStr)
            putExtra("VIBRATION_ENABLED", vibrationEnabled)
            putExtra("TIMER_ID", timerId)
            putExtra("TIMER_LABEL", timerLabel)
        }
        try {
            startActivity(intent)
        } catch (_: Exception) {
            // Some OEMs still block background activity starts; the posted
            // notification remains the fallback.
        }
    }

    /**
     * Schedules the next occurrence as soon as the alarm starts ringing. This
     * keeps repeating alarms alive even if the user never dismisses the alarm
     * or the activity is killed. One-time alarms are deactivated on dismiss
     * and therefore are not rescheduled here.
     */
    private fun scheduleNextOccurrence(alarmId: Int) {
        if (alarmId <= 0) return
        val app = application as? StrictClockApplication ?: return
        app.ioScope.launch {
            val alarm = app.repository.getAlarmById(alarmId) ?: return@launch
            if (!alarm.isActive) return@launch
            val isOneTime = !alarm.shiftEnabled && alarm.daysOfWeek == "Never"
            if (!isOneTime) {
                app.scheduler.schedule(alarm)
            }
        }
    }

    /**
     * If the alarm stream is muted or at the lowest step (some OEM ROMs report
     * zero as one), raise it to about half volume so the user cannot sleep
     * through a silent alarm. Normal audible volumes are left alone.
     */
    private fun ensureAlarmVolume() {
        try {
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            val current = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
            val muted = audioManager.isStreamMute(AudioManager.STREAM_ALARM)
            if (max > 0 && (current <= 1 || muted)) {
                val target = (max / 2).coerceAtLeast(1)
                audioManager.setStreamVolume(AudioManager.STREAM_ALARM, target, 0)
            }
        } catch (_: Exception) {
            // Some OEMs restrict volume changes; the alarm still plays.
        }
    }

    private fun playRingtone(soundUriStr: String) {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        try {
            val uri = if (soundUriStr.isNotEmpty() && soundUriStr != "Silent") {
                android.net.Uri.parse(soundUriStr)
            } else {
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            }
            if (soundUriStr == "Silent") return

            mediaPlayer = MediaPlayer().apply {
                setDataSource(this@AlarmService, uri)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val attrs = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                    setAudioAttributes(attrs)
                } else {
                    @Suppress("DEPRECATION")
                    setAudioStreamType(android.media.AudioManager.STREAM_ALARM)
                }
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startVibration() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        val pattern = longArrayOf(0, 1000, 1000)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(pattern, 0)
        }
    }

    override fun onDestroy() {
        if (runningInstance === this) {
            runningInstance = null
        }
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        vibrator?.cancel()
        vibrator = null
        _ringing.value = false
        currentIsTimer = false
        currentTimerId = -1
        currentTimerLabel = ""
        try {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } catch (_: Exception) {
            // The service may not have become foreground at all.
        }
        // notify() above is not tied to the foreground service lifecycle, so
        // cancel the alarm notification explicitly when the alarm is dismissed.
        getSystemService(NotificationManager::class.java)?.cancel(NOTIFICATION_ID)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
