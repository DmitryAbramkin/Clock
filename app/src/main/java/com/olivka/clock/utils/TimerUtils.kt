package com.olivka.clock.utils

import com.olivka.clock.data.TimerEntity

object TimerUtils {
    /**
     * Two timers are duplicates when they were created with the same duration
     * and the same label, mirroring the duplicate check of alarms.
     */
    fun isSameTimer(a: TimerEntity, b: TimerEntity): Boolean {
        return a.durationMs == b.durationMs &&
            a.label.trim().equals(b.label.trim(), ignoreCase = true)
    }

    /**
     * Compact countdown format used by the list and the detail screen:
     * `2:30` below one hour, `1:02:03` above. Rounds up so a freshly started
     * timer still shows its full duration instead of one second less.
     */
    fun formatRemaining(remainingMs: Long): String {
        val totalSeconds = (remainingMs.coerceAtLeast(0L) + 999L) / 1000L
        val hours = totalSeconds / 3600L
        val minutes = (totalSeconds % 3600L) / 60L
        val seconds = totalSeconds % 60L
        return if (hours > 0L) {
            String.format(java.util.Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(java.util.Locale.US, "%d:%02d", minutes, seconds)
        }
    }

    fun progress(remainingMs: Long, durationMs: Long): Float {
        if (durationMs <= 0L) return 0f
        return (remainingMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    }

    /**
     * Formats a configured duration for the timer page title, e.g.
     * `1ч 30м 20с` / `1h 30m 20s`. Zero-valued units are omitted; a zero
     * duration is shown as one zero unit.
     */
    fun formatDuration(
        durationMs: Long,
        hoursSuffix: String,
        minutesSuffix: String,
        secondsSuffix: String
    ): String {
        val totalSeconds = (durationMs.coerceAtLeast(0L)) / 1000L
        val hours = totalSeconds / 3600L
        val minutes = (totalSeconds % 3600L) / 60L
        val seconds = totalSeconds % 60L
        val parts = buildList {
            if (hours > 0L) add("$hours$hoursSuffix")
            if (minutes > 0L) add("$minutes$minutesSuffix")
            if (seconds > 0L) add("$seconds$secondsSuffix")
        }
        return if (parts.isEmpty()) "0$secondsSuffix" else parts.joinToString(" ")
    }
}
