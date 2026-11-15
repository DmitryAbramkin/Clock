package com.olivka.clock.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.olivka.clock.MainActivity
import com.olivka.clock.R
import com.olivka.clock.StrictClockApplication
import com.olivka.clock.data.TimerEntity
import com.olivka.clock.utils.TimerUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Runs any number of independent countdown timers.
 *
 * Every timer has its own notification and its own exact AlarmManager alarm.
 * While at least one timer is running the service is foreground (the first
 * running timer's notification is promoted); paused timers keep their
 * notifications and the service stops when no timers are left. The exact alarm
 * is still the safety net if the process is killed: [AlarmReceiver] starts
 * [AlarmService] directly.
 */
class TimerService : Service() {
    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    private val stateMutex = Mutex()
    private var tickJob: Job? = null
    private var notificationManager: NotificationManager? = null
    private var loaded = false
    private var foregroundTimerId: Int? = null
    private val timers = linkedMapOf<Int, TimerEntity>()
    private val lastNotifiedSecond = mutableMapOf<Int, Long>()

    private val prefs by lazy {
        getSharedPreferences("strict_clock_prefs", MODE_PRIVATE)
    }

    private val repository by lazy {
        (application as StrictClockApplication).timerRepository
    }

    private val scheduler by lazy {
        (application as StrictClockApplication).scheduler
    }

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_RESUME = "ACTION_RESUME"
        const val ACTION_PAUSE = "ACTION_PAUSE"
        const val ACTION_ADD_MINUTE = "ACTION_ADD_MINUTE"
        const val ACTION_RESET = "ACTION_RESET"
        const val ACTION_DELETE = "ACTION_DELETE"
        const val ACTION_RESTORE = "ACTION_RESTORE"
        const val EXTRA_TIMER_ID = "EXTRA_TIMER_ID"

        private const val CHANNEL_ID = "timer_channel_v4"
        private const val NOTIFICATION_BASE = 2000
        private const val PREF_ALARM_ACTIVE_PREFIX = "timer_alarm_active_"

        fun alarmActiveKey(timerId: Int): String = "$PREF_ALARM_ACTIVE_PREFIX$timerId"

        /**
         * Atomically consumes the one-shot exact alarm of a timer. Both the
         * foreground countdown and [AlarmReceiver] call this, so a timer can
         * never ring twice when the process and the alarm race.
         */
        @Synchronized
        fun claimAlarm(prefs: SharedPreferences, timerId: Int): Boolean {
            val key = alarmActiveKey(timerId)
            if (!prefs.getBoolean(key, false)) return false
            prefs.edit().putBoolean(key, false).commit()
            return true
        }

        fun start(context: Context, action: String, timerId: Int) {
            val intent = Intent(context, TimerService::class.java).apply {
                this.action = action
                putExtra(EXTRA_TIMER_ID, timerId)
            }
            context.startService(intent)
        }

        fun start(context: Context, action: String) {
            context.startService(Intent(context, TimerService::class.java).setAction(action))
        }
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(NotificationManager::class.java)
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // v4 is a new id on purpose: channels are immutable once created.
            // It mirrors the stopwatch channel: visible and tappable, but with
            // no sound or vibration on start/pause. Older channels were either
            // silent (v2) or could alert with the default sound (v3).
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.timer_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = getString(R.string.timer_channel_description)
                setSound(null, null)
                enableVibration(false)
                setShowBadge(false)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            notificationManager?.createNotificationChannel(channel)
            notificationManager?.deleteNotificationChannel("timer_channel_v3")
            notificationManager?.deleteNotificationChannel("timer_channel_v2")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            stopSelf(startId)
            return START_NOT_STICKY
        }
        scope.launch {
            try {
                stateMutex.withLock {
                    loadTimersIfNeeded()
                    handleAction(intent)
                }
            } catch (_: Exception) {
                // The database may be unavailable while the device is locked.
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private suspend fun loadTimersIfNeeded() {
        if (loaded) return
        val stored = repository.allTimers.first()
        timers.clear()
        stored.forEach { timers[it.id] = it }
        loaded = true
    }

    private suspend fun handleAction(intent: Intent) {
        val timerId = intent.getIntExtra(EXTRA_TIMER_ID, -1)
        when (intent.action) {
            ACTION_START, ACTION_RESUME -> startTimer(timerId)
            ACTION_PAUSE -> pauseTimer(timerId)
            ACTION_ADD_MINUTE -> addMinute(timerId)
            ACTION_RESET -> resetTimer(timerId)
            ACTION_DELETE -> deleteTimer(timerId)
            ACTION_RESTORE -> restoreTimers()
            else -> syncNotifications()
        }
        if (timers.values.any { it.isRunning }) {
            ensureTicking()
        }
    }

    private suspend fun startTimer(timerId: Int) {
        val timer = findTimer(timerId) ?: run {
            syncNotifications()
            return
        }
        if (timer.isRunning) {
            syncNotifications()
            return
        }
        val now = System.currentTimeMillis()
        val remaining = if (timer.remainingMs > 0L) {
            timer.remainingMs
        } else {
            // A finished timer that was kept after another alarm was already
            // ringing starts over instead of immediately ringing again.
            timer.durationMs
        }
        if (remaining <= 0L) {
            syncNotifications()
            return
        }
        val updated = timer.copy(
            remainingMs = remaining,
            isRunning = true,
            endAtMillis = now + remaining,
            hasStarted = true
        )
        save(updated)
        markAlarmActive(timerId, true)
        scheduler.scheduleTimer(
            timerId,
            remaining,
            timer.label,
            timer.soundUri,
            timer.vibrationEnabled,
            timer.vibrationFollowsGlobal
        )
        syncNotifications()
    }

    private suspend fun pauseTimer(timerId: Int) {
        val timer = findTimer(timerId) ?: run {
            syncNotifications()
            return
        }
        if (!timer.isRunning) {
            syncNotifications()
            return
        }
        val remaining = remainingOf(timer)
        if (remaining <= 0L) {
            finishTimer(timer)
            return
        }
        val updated = timer.copy(
            remainingMs = remaining,
            isRunning = false,
            endAtMillis = 0L,
            hasStarted = true
        )
        save(updated)
        markAlarmActive(timerId, false)
        scheduler.cancelTimer(timerId)
        syncNotifications()
    }

    private suspend fun addMinute(timerId: Int) {
        val timer = findTimer(timerId) ?: run {
            syncNotifications()
            return
        }
        val extra = 60_000L
        val newDuration = timer.durationMs + extra
        val updated = if (timer.isRunning) {
            val remaining = remainingOf(timer) + extra
            val newTimer = timer.copy(
                durationMs = newDuration,
                remainingMs = remaining,
                endAtMillis = System.currentTimeMillis() + remaining
            )
            markAlarmActive(timerId, true)
            scheduler.scheduleTimer(
                timerId,
                remaining,
                timer.label,
                timer.soundUri,
                timer.vibrationEnabled,
                timer.vibrationFollowsGlobal
            )
            newTimer
        } else {
            timer.copy(
                durationMs = newDuration,
                remainingMs = timer.remainingMs + extra
            )
        }
        save(updated)
        syncNotifications()
    }

    private suspend fun resetTimer(timerId: Int) {
        val timer = findTimer(timerId) ?: run {
            syncNotifications()
            return
        }
        val updated = timer.copy(
            remainingMs = timer.durationMs,
            isRunning = false,
            endAtMillis = 0L,
            hasStarted = false
        )
        save(updated)
        markAlarmActive(timerId, false)
        scheduler.cancelTimer(timerId)
        syncNotifications()
    }

    private suspend fun deleteTimer(timerId: Int) {
        val timer = timers.remove(timerId) ?: run {
            syncNotifications()
            return
        }
        markAlarmActive(timerId, false)
        scheduler.cancelTimer(timerId)
        lastNotifiedSecond.remove(timerId)
        notificationManager?.cancel(notificationId(timerId))
        runCatching { repository.delete(timer) }
        syncNotifications()
    }

    /**
     * Called when the app is opened and timers may have been counting while
     * the process was dead. Live timers are re-armed; timers that already
     * finished are either rung (when their exact alarm never fired) or left
     * as a "time's up" card when another alarm is ringing.
     */
    private suspend fun restoreTimers() {
        val now = System.currentTimeMillis()
        timers.values.toList().forEach { timer ->
            if (!timer.isRunning) return@forEach
            val remaining = timer.endAtMillis - now
            if (remaining > 0L) {
                val updated = timer.copy(
                    remainingMs = remaining,
                    hasStarted = true
                )
                timers[timer.id] = updated
                save(updated)
                markAlarmActive(timer.id, true)
                scheduler.scheduleTimer(
                    timer.id,
                    remaining,
                    timer.label,
                    timer.soundUri,
                    timer.vibrationEnabled,
                    timer.vibrationFollowsGlobal
                )
            } else {
                val alarmStillQueued = prefs.getBoolean(alarmActiveKey(timer.id), false)
                val finished = timer.copy(
                    remainingMs = 0L,
                    isRunning = false,
                    endAtMillis = 0L
                )
                if (AlarmService.isRinging) {
                    // Another alarm owns the screen; keep a dismissible card.
                    timers[timer.id] = finished
                    save(finished)
                    notificationManager?.notify(
                        notificationId(timer.id),
                        buildNotification(finished, timeUp = true)
                    )
                } else if (alarmStillQueued) {
                    finishTimer(finished)
                } else {
                    timers.remove(timer.id)
                    notificationManager?.cancel(notificationId(timer.id))
                    runCatching { repository.delete(timer) }
                }
            }
        }
        syncNotifications()
    }

    private fun ensureTicking() {
        if (timers.values.none { it.isRunning }) return
        // Restart the loop so a tick that is just finishing cannot leave a
        // newly started timer without a countdown.
        tickJob?.cancel()
        tickJob = scope.launch {
            while (isActive) {
                val keepRunning = stateMutex.withLock { tickOnce() }
                if (!keepRunning) break
                delay(500L)
            }
        }
    }

    /** Returns true while at least one timer is still running. */
    private suspend fun tickOnce(): Boolean {
        val now = System.currentTimeMillis()
        var running = false
        timers.values.toList().forEach { timer ->
            if (!timer.isRunning) return@forEach
            val remaining = timer.endAtMillis - now
            if (remaining <= 0L) {
                finishTimer(timer)
                return@forEach
            }
            running = true
            val updated = timer.copy(remainingMs = remaining)
            timers[timer.id] = updated
            if (lastNotifiedSecond[timer.id] != remaining / 1000L) {
                lastNotifiedSecond[timer.id] = remaining / 1000L
                notificationManager?.notify(
                    notificationId(timer.id),
                    buildNotification(updated)
                )
            }
        }
        if (!running) {
            syncNotifications()
        }
        return running
    }

    /**
     * Ends a timer and starts the ringing service. When another alarm is
     * already ringing the finished timer stays visible as a "time's up" card,
     * so its label is not silently lost; deleting it cancels the card.
     */
    private suspend fun finishTimer(timer: TimerEntity) {
        val current = timers[timer.id] ?: return
        if (!current.isRunning && current.remainingMs <= 0L) return
        val finished = current.copy(
            remainingMs = 0L,
            isRunning = false,
            endAtMillis = 0L,
            hasStarted = true
        )
        // Claim the one-shot exact alarm before clearing its flag: the
        // receiver and this countdown race to be the one that rings.
        val claimed = claimAlarm(prefs, finished.id)
        markAlarmActive(finished.id, false)
        scheduler.cancelTimer(finished.id)
        lastNotifiedSecond.remove(finished.id)

        if (AlarmService.isRinging) {
            timers[finished.id] = finished
            save(finished)
            notificationManager?.notify(
                notificationId(finished.id),
                buildNotification(finished, timeUp = true)
            )
            syncNotifications()
            return
        }

        if (claimed && startRingingAlarm(finished)) {
            // The wake-up service now owns the id and the label; the timer
            // itself has done its job and must not appear in the list again.
            timers.remove(finished.id)
            notificationManager?.cancel(notificationId(finished.id))
            runCatching { repository.delete(finished) }
        } else if (!claimed) {
            // The exact alarm won the race and already started the ring.
            timers.remove(finished.id)
            notificationManager?.cancel(notificationId(finished.id))
            runCatching { repository.delete(finished) }
        }
        // When startRingingAlarm() failed it kept the finished card and its
        // "time's up" notification as the fallback.
        syncNotifications()
    }

    private suspend fun startRingingAlarm(timer: TimerEntity): Boolean {
        val intent = Intent(this, AlarmService::class.java).apply {
            putExtra("ALARM_ID", -1)
            putExtra("IS_TIMER", true)
            putExtra("TIMER_ID", timer.id)
            putExtra("TIMER_LABEL", timer.label)
            putExtra("SOUND_URI", timer.soundUri)
            putExtra("VIBRATION_ENABLED", timer.vibrationEnabled)
            putExtra("VIBRATION_FOLLOWS_GLOBAL", timer.vibrationFollowsGlobal)
        }
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
            true
        } catch (_: Exception) {
            // Some OEM builds restrict foreground services. Keep the timer
            // card and its notification as the fallback UI.
            timers[timer.id] = timer
            runCatching { repository.update(timer) }
            notificationManager?.notify(
                notificationId(timer.id),
                buildNotification(timer, timeUp = true)
            )
            false
        }
    }

    private suspend fun findTimer(timerId: Int): TimerEntity? {
        timers[timerId]?.let { return it }
        val stored = repository.getTimerById(timerId) ?: return null
        timers[stored.id] = stored
        return stored
    }

    private suspend fun save(timer: TimerEntity) {
        timers[timer.id] = timer
        runCatching { repository.update(timer) }
    }

    private fun markAlarmActive(timerId: Int, active: Boolean) {
        prefs.edit().putBoolean(alarmActiveKey(timerId), active).apply()
    }

    private fun remainingOf(timer: TimerEntity): Long {
        return if (timer.isRunning) {
            (timer.endAtMillis - System.currentTimeMillis()).coerceAtLeast(0L)
        } else {
            timer.remainingMs.coerceAtLeast(0L)
        }
    }

    /**
     * Publishes one notification per timer and promotes the first running
     * timer to the foreground notification. When no timer is running the
     * service detaches the foreground notification and re-posts the paused
     * ones. The service itself stops when no timers are left.
     */
    private fun syncNotifications() {
        val snapshot = timers.values.toList()
        val running = snapshot.filter { it.isRunning && remainingOf(it) > 0L }
        val withNotification = snapshot

        withNotification.forEach { timer ->
            val timeUp = !timer.isRunning && timer.remainingMs <= 0L
            notificationManager?.notify(
                notificationId(timer.id),
                buildNotification(timer, timeUp = timeUp)
            )
        }

        val firstRunning = running.firstOrNull()
        if (firstRunning != null) {
            if (foregroundTimerId != firstRunning.id) {
                val oldId = foregroundTimerId
                try {
                    startForeground(
                        notificationId(firstRunning.id),
                        buildNotification(firstRunning)
                    )
                    foregroundTimerId = firstRunning.id
                } catch (_: Exception) {
                    foregroundTimerId = null
                }
                if (oldId != null && oldId != firstRunning.id) {
                    notificationManager?.cancel(notificationId(oldId))
                    timers[oldId]?.let { old ->
                        val oldTimeUp = !old.isRunning && old.remainingMs <= 0L
                        notificationManager?.notify(
                            notificationId(old.id),
                            buildNotification(old, timeUp = oldTimeUp)
                        )
                    }
                }
            }
        } else if (foregroundTimerId != null) {
            try {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } catch (_: Exception) {
                // The service may not have become foreground at all.
            }
            foregroundTimerId = null
            // The foreground notification was just removed; restore paused
            // timer notifications that should stay in the shade.
            withNotification.forEach { timer ->
                val timeUp = !timer.isRunning && timer.remainingMs <= 0L
                notificationManager?.notify(
                    notificationId(timer.id),
                    buildNotification(timer, timeUp = timeUp)
                )
            }
        }

        if (timers.isEmpty()) {
            stopSelf()
        }
    }

    private fun notificationId(timerId: Int): Int = NOTIFICATION_BASE + timerId

    private fun contentIntent(timerId: Int): PendingIntent {
        return PendingIntent.getActivity(
            this,
            timerId * 10 + 7,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("OPEN_SCREEN", "TimerDetail")
                putExtra("OPEN_TIMER_ID", timerId)
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun actionIntent(
        timerId: Int,
        action: String,
        requestOffset: Int
    ): PendingIntent {
        val intent = Intent(this, TimerService::class.java).apply {
            this.action = action
            putExtra(EXTRA_TIMER_ID, timerId)
        }
        return PendingIntent.getService(
            this,
            timerId * 10 + requestOffset,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun buildNotification(
        timer: TimerEntity,
        timeUp: Boolean = false
    ): android.app.Notification {
        val remaining = remainingOf(timer)
        val progress = (TimerUtils.progress(remaining, timer.durationMs) * 100)
            .toInt()
            .coerceIn(0, 100)
        val title = timer.label.ifBlank { getString(R.string.timer) }
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(
                if (timeUp) {
                    getString(R.string.timer_finished)
                } else {
                    getString(R.string.timer_notification_text)
                }
            )
            .setSmallIcon(android.R.drawable.ic_menu_recent_history)
            .setProgress(100, progress, false)
            .setContentIntent(contentIntent(timer.id))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)

        if (timer.isRunning && !timeUp) {
            builder
                .setWhen(timer.endAtMillis)
                .setShowWhen(true)
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
        } else {
            builder.setShowWhen(false)
            if (!timeUp) {
                builder.setContentText(TimerUtils.formatRemaining(remaining))
            }
        }

        if (timeUp) {
            builder.addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                getString(R.string.delete),
                actionIntent(timer.id, ACTION_DELETE, 4)
            )
            return builder.build()
        }

        if (timer.isRunning) {
            builder.addAction(
                android.R.drawable.ic_media_pause,
                getString(R.string.pause),
                actionIntent(timer.id, ACTION_PAUSE, 1)
            )
            builder.addAction(
                android.R.drawable.ic_menu_add,
                getString(R.string.add_minute),
                actionIntent(timer.id, ACTION_ADD_MINUTE, 2)
            )
        } else {
            builder.addAction(
                android.R.drawable.ic_media_play,
                getString(R.string.resume),
                actionIntent(timer.id, ACTION_RESUME, 3)
            )
            builder.addAction(
                android.R.drawable.ic_menu_add,
                getString(R.string.add_minute),
                actionIntent(timer.id, ACTION_ADD_MINUTE, 2)
            )
        }
        builder.addAction(
            android.R.drawable.ic_menu_close_clear_cancel,
            getString(R.string.cancel),
            actionIntent(timer.id, ACTION_DELETE, 4)
        )
        return builder.build()
    }

    override fun onDestroy() {
        // Do not cancel exact alarms here: if the OS kills the process they
        // are the only thing that can still ring the timers.
        tickJob?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
