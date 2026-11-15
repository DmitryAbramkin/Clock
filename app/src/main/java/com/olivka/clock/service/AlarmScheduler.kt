package com.olivka.clock.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.olivka.clock.data.AlarmEntity
import com.olivka.clock.utils.AlarmUtils

class AlarmScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun schedule(alarm: AlarmEntity) {
        if (!alarm.isActive) {
            cancel(alarm)
            return
        }

        val triggerTime = AlarmUtils.getNextTriggerTime(alarm) ?: run {
            cancel(alarm)
            return
        }
        val pendingIntent = alarmPendingIntent(
            requestCode = alarm.id,
            alarmId = alarm.id,
            soundUri = alarm.soundUri,
            vibrationEnabled = alarm.vibrationEnabled,
            vibrationFollowsGlobal = alarm.vibrationFollowsGlobal
        )

        scheduleExact(triggerTime, pendingIntent)
    }

    fun scheduleSnooze(
        alarmId: Int,
        soundUri: String,
        vibrationEnabled: Boolean,
        minutes: Int
    ) {
        if (alarmId <= 0) return
        val safeMinutes = minutes.coerceIn(1, 30)
        val triggerTime = System.currentTimeMillis() + safeMinutes * 60_000L
        val pendingIntent = alarmPendingIntent(
            requestCode = snoozeRequestCode(alarmId),
            alarmId = alarmId,
            soundUri = soundUri,
            vibrationEnabled = vibrationEnabled,
            vibrationFollowsGlobal = false
        )

        scheduleExact(triggerTime, pendingIntent)
    }

    private fun scheduleExact(triggerTime: Long, pendingIntent: PendingIntent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            !alarmManager.canScheduleExactAlarms()
        ) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
            return
        }
        try {
            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(triggerTime, pendingIntent),
                pendingIntent
            )
        } catch (_: SecurityException) {
            // Exact alarms were revoked after the schedule was created.
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        }
    }

    fun cancel(alarm: AlarmEntity) {
        alarmManager.cancel(cancelPendingIntent(alarm.id))
        alarmManager.cancel(cancelPendingIntent(snoozeRequestCode(alarm.id)))
    }

    /**
     * Backstop for the timer foreground service: fires [AlarmReceiver] even if
     * the app process was killed or frozen by the OEM battery manager.
     * Every timer owns its request code, so several timers can be scheduled
     * independently. Starting or resuming a timer replaces its previous alarm.
     */
    fun scheduleTimer(
        timerId: Int,
        remainingMs: Long,
        label: String,
        soundUri: String,
        vibrationEnabled: Boolean,
        vibrationFollowsGlobal: Boolean
    ) {
        val triggerTime = System.currentTimeMillis() + remainingMs.coerceAtLeast(0L)
        val pendingIntent = timerPendingIntent(
            timerId,
            label,
            soundUri,
            vibrationEnabled,
            vibrationFollowsGlobal
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            !alarmManager.canScheduleExactAlarms()
        ) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
            return
        }
        try {
            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(triggerTime, pendingIntent),
                pendingIntent
            )
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        }
    }

    fun cancelTimer(timerId: Int) {
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            TIMER_REQUEST_OFFSET + timerId,
            Intent(context, AlarmReceiver::class.java).setAction(ACTION_TIMER_ALARM),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    private fun timerPendingIntent(
        timerId: Int,
        label: String,
        soundUri: String,
        vibrationEnabled: Boolean,
        vibrationFollowsGlobal: Boolean
    ): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_TIMER_ALARM
            putExtra("ALARM_ID", -1)
            putExtra("IS_TIMER", true)
            putExtra("TIMER_ID", timerId)
            putExtra("TIMER_LABEL", label)
            putExtra("SOUND_URI", soundUri)
            putExtra("VIBRATION_ENABLED", vibrationEnabled)
            putExtra("VIBRATION_FOLLOWS_GLOBAL", vibrationFollowsGlobal)
        }
        return PendingIntent.getBroadcast(
            context,
            TIMER_REQUEST_OFFSET + timerId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun alarmPendingIntent(
        requestCode: Int,
        alarmId: Int,
        soundUri: String,
        vibrationEnabled: Boolean,
        vibrationFollowsGlobal: Boolean
    ): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("ALARM_ID", alarmId)
            putExtra("SOUND_URI", soundUri)
            putExtra("VIBRATION_ENABLED", vibrationEnabled)
            putExtra("VIBRATION_FOLLOWS_GLOBAL", vibrationFollowsGlobal)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun cancelPendingIntent(requestCode: Int): PendingIntent {
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            Intent(context, AlarmReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        private const val SNOOZE_REQUEST_OFFSET = 100_000
        // Well above the alarm id and snooze request ranges. Every timer gets
        // its own request code so exact alarms do not overwrite each other.
        private const val TIMER_REQUEST_OFFSET = 900_000
        private const val ACTION_TIMER_ALARM = "ACTION_TIMER_ALARM"
        fun snoozeRequestCode(alarmId: Int): Int = alarmId + SNOOZE_REQUEST_OFFSET
    }
}
