package com.olivka.clock.utils

import android.content.Context
import android.provider.Settings

/**
 * Snapshot of the system switches that gate app vibration:
 * the master vibration setting and touch feedback.
 */
data class SystemVibrationState(
    val masterEnabled: Boolean,
    val touchEnabled: Boolean
) {
    /** Wheel feedback needs both the master switch and touch feedback. */
    val wheelFeedbackEnabled: Boolean get() = masterEnabled && touchEnabled
}

/**
 * Reads the same settings that AOSP VibratorManagerService uses to decide
 * whether an app vibration is allowed. HapticFeedbackEnabled is the legacy
 * flag; HapticFeedbackIntensity is the modern one and can be 0 (off).
 *
 * The system "Alarm vibration" switch is stored in the hidden
 * alarm_vibration_intensity key, which normal apps cannot read on Android 12+.
 */
fun readSystemVibrationState(context: Context): SystemVibrationState {
    val resolver = context.contentResolver
    val masterEnabled = Settings.System.getInt(
        resolver,
        Settings.System.VIBRATE_ON,
        1
    ) != 0
    val touchFlagEnabled = Settings.System.getInt(
        resolver,
        "haptic_feedback_enabled",
        1
    ) != 0
    val touchIntensity = Settings.System.getInt(
        resolver,
        "haptic_feedback_intensity",
        -1
    )
    return SystemVibrationState(
        masterEnabled = masterEnabled,
        touchEnabled = touchFlagEnabled && touchIntensity != 0
    )
}
