package com.olivka.clock.utils

import com.olivka.clock.data.AlarmEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class AlarmUtilsTest {

    private val zone = ZoneId.of("Europe/Moscow")

    private fun at(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int = 0
    ): ZonedDateTime = ZonedDateTime.of(year, month, day, hour, minute, 0, 0, zone)

    private fun epoch(value: ZonedDateTime): Long = value.toInstant().toEpochMilli()

    private fun alarm(
        hour: Int = 7,
        minute: Int = 0,
        days: String = "Never",
        shift: Boolean = false,
        shiftLength: Int = 4,
        shiftStart: String = "",
        shiftSlots: String = "",
        skip: Long = 0L
    ): AlarmEntity = AlarmEntity(
        timeHour = hour,
        timeMinute = minute,
        daysOfWeek = days,
        isActive = true,
        shiftEnabled = shift,
        shiftLengthDays = shiftLength,
        shiftStartDate = shiftStart,
        shiftSlots = shiftSlots,
        skipNextTriggerAt = skip
    )

    @Test
    fun daily_beforeTime_returnsToday() {
        val result = AlarmUtils.getNextTriggerTime(
            alarm(days = "Daily"),
            at(2026, 10, 26, 6)
        )
        assertEquals(epoch(at(2026, 10, 26, 7)), result)
    }

    @Test
    fun daily_afterTime_returnsTomorrow() {
        val result = AlarmUtils.getNextTriggerTime(
            alarm(days = "Daily"),
            at(2026, 10, 26, 8)
        )
        assertEquals(epoch(at(2026, 10, 27, 7)), result)
    }

    @Test
    fun weekdays_fridayAfterTime_returnsMonday() {
        val result = AlarmUtils.getNextTriggerTime(
            alarm(days = "Mon-Fri"),
            at(2026, 10, 30, 8)
        )
        assertEquals(epoch(at(2026, 11, 2, 7)), result)
    }

    @Test
    fun customDays_skipsDisabledDays() {
        val result = AlarmUtils.getNextTriggerTime(
            alarm(days = "Mon, Wed, Fri"),
            at(2026, 10, 27, 8) // Tuesday
        )
        assertEquals(epoch(at(2026, 10, 28, 7)), result)
    }

    @Test
    fun shift_startToday_beforeTime_returnsToday() {
        val result = AlarmUtils.getNextTriggerTime(
            alarm(
                shift = true,
                shiftStart = "2026-10-26",
                shiftSlots = "420,420,-1,-1"
            ),
            at(2026, 10, 26, 6)
        )
        assertEquals(epoch(at(2026, 10, 26, 7)), result)
    }

    @Test
    fun shift_startToday_afterTime_returnsNextEnabledDay() {
        val result = AlarmUtils.getNextTriggerTime(
            alarm(
                shift = true,
                shiftStart = "2026-10-26",
                shiftSlots = "420,420,-1,-1"
            ),
            at(2026, 10, 26, 8)
        )
        assertEquals(epoch(at(2026, 10, 27, 7)), result)
    }

    @Test
    fun shift_skipsOffDaysAndWrapsCycle() {
        val result = AlarmUtils.getNextTriggerTime(
            alarm(
                shift = true,
                shiftStart = "2026-10-26",
                shiftSlots = "420,420,-1,-1"
            ),
            at(2026, 10, 27, 8)
        )
        assertEquals(epoch(at(2026, 10, 30, 7)), result)
    }

    @Test
    fun shift_allDaysOff_returnsNull() {
        val result = AlarmUtils.getNextTriggerTime(
            alarm(
                shift = true,
                shiftStart = "2026-10-26",
                shiftSlots = "-1,-1,-1,-1"
            ),
            at(2026, 10, 26, 6)
        )
        assertNull(result)
    }

    @Test
    fun shift_startInFuture_returnsStartDate() {
        val result = AlarmUtils.getNextTriggerTime(
            alarm(
                shift = true,
                shiftStart = "2026-11-01",
                shiftSlots = "420,420,-1,-1"
            ),
            at(2026, 10, 26, 6)
        )
        assertEquals(epoch(at(2026, 11, 1, 7)), result)
    }

    @Test
    fun shift_startInPast_usesModuloIndex() {
        // Only day index 0 is enabled. Start Oct 1, now Oct 26 -> index 1.
        // Next index 0 is Oct 29.
        val result = AlarmUtils.getNextTriggerTime(
            alarm(
                shift = true,
                shiftStart = "2026-10-01",
                shiftSlots = "420,-1,-1,-1"
            ),
            at(2026, 10, 26, 6)
        )
        assertEquals(epoch(at(2026, 10, 29, 7)), result)
    }

    @Test
    fun shift_length60_lastSlotEnabled() {
        val slots = MutableList<String>(60) { "-1" }
        slots[59] = "360" // 06:00
        val start = LocalDate.of(2026, 10, 26)
        val expectedDate = start.plusDays(59)
        val result = AlarmUtils.getNextTriggerTime(
            alarm(
                shift = true,
                shiftLength = 60,
                shiftStart = start.toString(),
                shiftSlots = slots.joinToString(",")
            ),
            at(2026, 10, 26, 5)
        )
        assertEquals(
            epoch(
                ZonedDateTime.of(
                    expectedDate.year,
                    expectedDate.monthValue,
                    expectedDate.dayOfMonth,
                    6,
                    0,
                    0,
                    0,
                    zone
                )
            ),
            result
        )
    }

    @Test
    fun shift_handicap_dstTransition() {
        // Europe/Berlin moves to summer time on 2026-03-29 at 02:00.
        // 02:30 does not exist; ZonedDateTime resolves it forward to 03:30.
        val berlin = ZoneId.of("Europe/Berlin")
        val now = ZonedDateTime.of(2026, 3, 29, 0, 0, 0, 0, berlin)
        val result = AlarmUtils.getNextTriggerTime(
            alarm(
                shift = true,
                shiftStart = "2026-03-29",
                shiftSlots = "150,150,-1,-1"
            ),
            now
        )
        val expected = ZonedDateTime.of(2026, 3, 29, 3, 30, 0, 0, berlin)
        assertEquals(expected.toInstant().toEpochMilli(), result)
    }

    @Test
    fun parseAndEncodeShiftSlots_roundTrip() {
        val values = listOf<Int?>(420, null, 0, 1439)
        val encoded = AlarmUtils.encodeShiftSlots(values)
        assertEquals("420,-1,0,1439", encoded)
        assertEquals(values, AlarmUtils.parseShiftSlots(encoded, 4))
    }

    @Test
    fun parseShiftSlots_invalidAndMissingValuesBecomeOff() {
        assertEquals(
            listOf<Int?>(null, null, null),
            AlarmUtils.parseShiftSlots("1440,abc", 3)
        )
    }

    @Test
    fun sameSchedule_onceDuplicates() {
        assertTrue(
            AlarmUtils.isSameSchedule(
                alarm(hour = 21, minute = 0, days = "Never"),
                alarm(hour = 21, minute = 0, days = "Never")
            )
        )
        assertFalse(
            AlarmUtils.isSameSchedule(
                alarm(hour = 21, minute = 0, days = "Never"),
                alarm(hour = 21, minute = 5, days = "Never")
            )
        )
        assertFalse(
            AlarmUtils.isSameSchedule(
                alarm(hour = 21, minute = 0, days = "Never"),
                alarm(hour = 21, minute = 0, days = "Daily")
            )
        )
    }

    @Test
    fun sameSchedule_shiftDuplicatesIncludeCycle() {
        val base = alarm(
            hour = 7,
            days = "Never",
            shift = true,
            shiftStart = "2026-10-26",
            shiftSlots = "420,420,-1,-1"
        )
        assertTrue(AlarmUtils.isSameSchedule(base, base.copy()))
        assertFalse(
            AlarmUtils.isSameSchedule(
                base,
                base.copy(shiftStartDate = "2026-10-27")
            )
        )
        assertFalse(
            AlarmUtils.isSameSchedule(
                base,
                base.copy(shiftSlots = "420,-1,-1,-1")
            )
        )
    }

    @Test
    fun sameSchedule_nonShiftIgnoresShiftFields() {
        val a = alarm(hour = 21, minute = 0, days = "Never")
        val b = a.copy(shiftLengthDays = 14, shiftSlots = "1,2,3")
        assertTrue(AlarmUtils.isSameSchedule(a, b))
    }

    @Test
    fun skippedDailyTrigger_returnsFollowingDay() {
        val now = at(2026, 10, 26, 6)
        val next = epoch(at(2026, 10, 26, 7))
        val result = AlarmUtils.getNextTriggerTime(
            alarm(days = "Daily", skip = next),
            now
        )
        assertEquals(epoch(at(2026, 10, 27, 7)), result)
    }

    @Test
    fun skipInPast_isIgnored() {
        val now = at(2026, 10, 26, 8)
        val past = epoch(at(2026, 10, 25, 7))
        val result = AlarmUtils.getNextTriggerTime(
            alarm(days = "Daily", skip = past),
            now
        )
        assertEquals(epoch(at(2026, 10, 27, 7)), result)
    }

    @Test
    fun skippedShiftTrigger_returnsNextCycleDay() {
        val now = at(2026, 10, 26, 6)
        val next = epoch(at(2026, 10, 26, 7))
        val result = AlarmUtils.getNextTriggerTime(
            alarm(
                hour = 7,
                days = "Never",
                shift = true,
                shiftStart = "2026-10-26",
                shiftSlots = "420,420,-1,-1",
                skip = next
            ),
            now
        )
        assertEquals(epoch(at(2026, 10, 27, 7)), result)
    }
}