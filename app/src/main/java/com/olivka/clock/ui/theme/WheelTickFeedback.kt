package com.olivka.clock.ui.theme

import android.content.Context
import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import com.olivka.clock.utils.WheelHaptics

/** One wheel tick. Every wheel owns an instance, so the throttle is per wheel. */
internal interface WheelTickFeedback {
    fun tick()
}

// A fast fling crosses many values; keep the platform ticks light.
private const val HAPTIC_TICK_INTERVAL_MS = 40L

// Direct pulses are stronger than platform haptics, so they tick less often.
private const val FALLBACK_TICK_INTERVAL_MS = 140L

/**
 * Picks the wheel feedback implementation for this device. Haptic-capable
 * phones keep the system haptic event; phones with only a plain vibrator get
 * a short low-power pulse from the separate fallback implementation.
 */
@Composable
internal fun rememberWheelTickFeedback(enabled: Boolean): WheelTickFeedback {
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    val hapticSupported = remember(context) { WheelHaptics.hasHapticFeedback(context) }
    val vibratorAvailable = remember(context) { WheelHaptics.hasVibrator(context) }
    return remember(enabled, hapticSupported, vibratorAvailable, hapticFeedback) {
        when {
            !enabled -> NoopWheelTickFeedback
            hapticSupported -> HapticWheelTickFeedback(hapticFeedback)
            vibratorAvailable -> LinearMotorWheelTickFeedback(context.applicationContext)
            else -> NoopWheelTickFeedback
        }
    }
}

private class HapticWheelTickFeedback(
    private val hapticFeedback: HapticFeedback
) : WheelTickFeedback {
    private var lastTickAt = 0L

    override fun tick() {
        val now = SystemClock.uptimeMillis()
        if (now - lastTickAt < HAPTIC_TICK_INTERVAL_MS) return
        lastTickAt = now
        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }
}

private class LinearMotorWheelTickFeedback(
    private val context: Context
) : WheelTickFeedback {
    private var lastTickAt = 0L

    override fun tick() {
        val now = SystemClock.uptimeMillis()
        if (now - lastTickAt < FALLBACK_TICK_INTERVAL_MS) return
        lastTickAt = now
        WheelHaptics.performBriefPulse(context)
    }
}

private object NoopWheelTickFeedback : WheelTickFeedback {
    override fun tick() = Unit
}
