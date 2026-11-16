package com.olivka.clock.utils

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Hardware checks and the direct-vibration fallback for the time wheels.
 *
 * Compose haptics go through the platform haptic feedback channel. Devices
 * with a plain vibration motor may ignore those events entirely, so the wheel
 * checks [hasHapticFeedback] first and uses [performBriefPulse] when only a
 * basic vibrator is available.
 */
object WheelHaptics {
    const val PREF_ENABLED = "wheel_haptics_enabled"

    // Long enough for a basic motor to spin up, short enough to stay a tick.
    private const val FALLBACK_PULSE_MS = 8L

    fun isEnabled(prefs: SharedPreferences): Boolean =
        prefs.getBoolean(PREF_ENABLED, true)

    /**
     * True when the device exposes a haptic actuator with amplitude control.
     * Plain vibration motors report false, and their platform haptic events
     * are often silent.
     */
    fun hasHapticFeedback(context: Context): Boolean {
        val vibrator = vibrator(context) ?: return false
        return vibrator.hasVibrator() && vibrator.hasAmplitudeControl()
    }

    fun hasVibrator(context: Context): Boolean =
        vibrator(context)?.hasVibrator() == true

    /**
     * A single low-power pulse for devices without a haptic actuator. Motors
     * without amplitude control ignore the amplitude, so the short duration is
     * what keeps the fallback from turning into a jackhammer. The platform
     * default amplitude is used because a literal amplitude of 1 is scaled to
     * zero by some HALs, which makes the pulse completely silent.
     */
    fun performBriefPulse(context: Context) {
        val vibrator = vibrator(context) ?: return
        if (!vibrator.hasVibrator()) return
        val effect = VibrationEffect.createOneShot(
            FALLBACK_PULSE_MS,
            VibrationEffect.DEFAULT_AMPLITUDE
        )
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                vibrator.vibrate(
                    effect,
                    VibrationAttributes.createForUsage(VibrationAttributes.USAGE_TOUCH)
                )
            } else {
                vibrator.vibrate(effect)
            }
        }
    }

    private fun vibrator(context: Context): Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)
                ?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
}
