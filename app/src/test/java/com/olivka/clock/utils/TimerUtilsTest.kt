package com.olivka.clock.utils

import com.olivka.clock.data.TimerEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimerUtilsTest {
    private fun timer(
        durationMs: Long = 5 * 60_000L,
        label: String = ""
    ) = TimerEntity(
        id = 1,
        label = label,
        durationMs = durationMs,
        remainingMs = durationMs
    )

    @Test
    fun duplicateWhenDurationAndLabelMatch() {
        assertTrue(
            TimerUtils.isSameTimer(
                timer(label = "Tea"),
                timer(label = " tea ")
            )
        )
        assertTrue(
            TimerUtils.isSameTimer(
                timer(label = ""),
                timer(label = "  ")
            )
        )
    }

    @Test
    fun differentDurationOrLabelIsNotDuplicate() {
        assertFalse(
            TimerUtils.isSameTimer(
                timer(durationMs = 5 * 60_000L, label = "Tea"),
                timer(durationMs = 6 * 60_000L, label = "Tea")
            )
        )
        assertFalse(
            TimerUtils.isSameTimer(
                timer(label = "Tea"),
                timer(label = "Coffee")
            )
        )
    }

    @Test
    fun formatsCountdownCompactly() {
        assertEquals("0:05", TimerUtils.formatRemaining(5_000L))
        assertEquals("2:30", TimerUtils.formatRemaining(2 * 60_000L + 30_000L))
        assertEquals("44:50", TimerUtils.formatRemaining(44 * 60_000L + 50_000L))
        assertEquals("1:02:03", TimerUtils.formatRemaining(3_723_000L))
    }

    @Test
    fun roundsUpDuringFinalSecond() {
        assertEquals("0:01", TimerUtils.formatRemaining(1L))
        assertEquals("0:00", TimerUtils.formatRemaining(0L))
    }

    @Test
    fun progressIsClamped() {
        assertEquals(1f, TimerUtils.progress(10_000L, 10_000L), 0.001f)
        assertEquals(0.5f, TimerUtils.progress(5_000L, 10_000L), 0.001f)
        assertEquals(0f, TimerUtils.progress(0L, 10_000L), 0.001f)
        assertEquals(0f, TimerUtils.progress(5_000L, 0L), 0.001f)
    }

    @Test
    fun formatsDurationForTimerTitle() {
        assertEquals(
            "1ч 30м 20с",
            TimerUtils.formatDuration(5_420_000L, "ч", "м", "с")
        )
        assertEquals("1ч", TimerUtils.formatDuration(3_600_000L, "ч", "м", "с"))
        assertEquals("5м", TimerUtils.formatDuration(300_000L, "ч", "м", "с"))
        assertEquals("30с", TimerUtils.formatDuration(30_000L, "ч", "м", "с"))
        assertEquals("0с", TimerUtils.formatDuration(0L, "ч", "м", "с"))
    }
}
