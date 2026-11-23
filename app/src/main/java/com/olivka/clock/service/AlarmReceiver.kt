package com.olivka.clock.service

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.olivka.clock.StrictClockApplication
import com.olivka.clock.utils.TimerUtils
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getIntExtra("ALARM_ID", -1)
        val soundUri = intent.getStringExtra("SOUND_URI") ?: ""
        val vibrationEnabled = intent.getBooleanExtra("VIBRATION_ENABLED", true)
        val vibrationFollowsGlobal = intent.getBooleanExtra(
            "VIBRATION_FOLLOWS_GLOBAL",
            false
        )
        val isTimer = intent.getBooleanExtra("IS_TIMER", false)

        if (isTimer) {
            val timerId = intent.getIntExtra("TIMER_ID", -1)
            if (timerId <= 0) return
            val prefs = context.getSharedPreferences(
                "strict_clock_prefs",
                Context.MODE_PRIVATE
            )
            // A ringing alarm has the user's attention; do not replace it.
            // The timer stays active and its exact alarm is re-created when
            // the user returns to the app.
            if (AlarmService.isRinging) return
            // The timer was paused or deleted after the alarm was queued.
            if (!TimerService.claimAlarm(prefs, timerId)) return

            // The countdown is over: its controls must leave the shade even
            // when the timer service was killed or is dozing.
            context.getSystemService(NotificationManager::class.java)
                ?.cancel(TimerService.notificationId(timerId))

            val serviceIntent = Intent(context, AlarmService::class.java).apply {
                putExtra("ALARM_ID", -1)
                putExtra("SOUND_URI", soundUri)
                putExtra("VIBRATION_ENABLED", vibrationEnabled)
                putExtra("VIBRATION_FOLLOWS_GLOBAL", vibrationFollowsGlobal)
                putExtra("IS_TIMER", true)
                putExtra("TIMER_ID", timerId)
                putExtra("TIMER_LABEL", intent.getStringExtra("TIMER_LABEL") ?: "")
            }
            startAlarmService(context, serviceIntent)

            // The ringing service already received the label, sound and
            // vibration, so resetting the stored row cannot affect the ring.
            // The timer stays in the list as if the user had reset it.
            val app = context.applicationContext as? StrictClockApplication
            app?.ioScope?.launch {
                val timer = app.timerRepository.getTimerById(timerId) ?: return@launch
                runCatching { app.timerRepository.update(TimerUtils.reset(timer)) }
            }
            return
        }

        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            putExtra("ALARM_ID", alarmId)
            putExtra("SOUND_URI", soundUri)
            putExtra("VIBRATION_ENABLED", vibrationEnabled)
            putExtra("VIBRATION_FOLLOWS_GLOBAL", vibrationFollowsGlobal)
            putExtra("IS_TIMER", false)
        }
        startAlarmService(context, serviceIntent)
    }

    private fun startAlarmService(context: Context, intent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }
}
