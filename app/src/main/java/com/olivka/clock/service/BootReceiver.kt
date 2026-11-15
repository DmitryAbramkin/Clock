package com.olivka.clock.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.olivka.clock.StrictClockApplication
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val supportedActions = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_USER_UNLOCKED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED
        )
        if (intent.action !in supportedActions) return

        val app = context.applicationContext as? StrictClockApplication ?: return
        val pendingResult = goAsync()
        app.ioScope.launch {
            try {
                val alarms = app.repository.allAlarms.first()
                alarms.forEach { alarm ->
                    if (alarm.isActive) {
                        app.scheduler.schedule(alarm)
                    }
                }
                val now = System.currentTimeMillis()
                app.timerRepository.allTimers.first().forEach { timer ->
                    if (timer.isRunning) {
                        val remaining = timer.endAtMillis - now
                        if (remaining > 0L) {
                            app.scheduler.scheduleTimer(
                                timer.id,
                                remaining,
                                timer.label,
                                timer.soundUri,
                                timer.vibrationEnabled,
                                timer.vibrationFollowsGlobal
                            )
                        }
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
