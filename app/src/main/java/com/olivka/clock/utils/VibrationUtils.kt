package com.olivka.clock.utils

import android.content.SharedPreferences

/**
 * Global vibration defaults used when creating a new alarm or timer. The
 * chosen values are then stored on the item itself, the same way the global
 * sound is copied into a new alarm/timer.
 */
object VibrationUtils {
    const val PREF_ALARM_DEFAULT = "default_alarm_vibration"
    const val PREF_TIMER_DEFAULT = "default_timer_vibration"

    fun globalAlarmEnabled(prefs: SharedPreferences): Boolean =
        prefs.getBoolean(PREF_ALARM_DEFAULT, true)

    fun globalTimerEnabled(prefs: SharedPreferences): Boolean =
        prefs.getBoolean(PREF_TIMER_DEFAULT, true)
}
