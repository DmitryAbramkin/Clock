package com.olivka.clock.utils

import android.content.Context
import com.olivka.clock.R
import com.olivka.clock.data.AlarmEntity
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

object AlarmUtils {
    const val MAX_SHIFT_DAYS = 60

    private val dayNameToIso = mapOf(
        "Mon" to 1,
        "Tue" to 2,
        "Wed" to 3,
        "Thu" to 4,
        "Fri" to 5,
        "Sat" to 6,
        "Sun" to 7
    )

    /**
     * Returns the next trigger time in epoch millis, or null when the alarm
     * has no future occurrence (for example, every shift day is disabled).
     */
    fun getNextTriggerTime(
        alarm: AlarmEntity,
        now: ZonedDateTime = ZonedDateTime.now()
    ): Long? {
        val next = nextTriggerRaw(alarm, now) ?: return null
        val skipped = alarm.skipNextTriggerAt
        if (skipped > 0 && next == skipped) {
            val afterSkipped = Instant.ofEpochMilli(skipped + 1_000)
                .atZone(now.zone)
            return nextTriggerRaw(alarm, afterSkipped)
        }
        return next
    }

    private fun nextTriggerRaw(
        alarm: AlarmEntity,
        now: ZonedDateTime
    ): Long? {
        if (alarm.shiftEnabled) {
            return nextShiftTrigger(alarm, now)
        }

        val time = safeTime(alarm.timeHour, alarm.timeMinute)
        return when (alarm.daysOfWeek) {
            "Never", "Daily" -> nextDaily(now, time)
            "Mon-Fri" -> nextMatchingDay(now, setOf(1, 2, 3, 4, 5), time)
            "Sat-Sun" -> nextMatchingDay(now, setOf(6, 7), time)
            else -> {
                val days = parseDays(alarm.daysOfWeek)
                if (days.isEmpty()) null else nextMatchingDay(now, days, time)
            }
        }
    }

    private fun nextDaily(now: ZonedDateTime, time: LocalTime): Long {
        var candidate = now.toLocalDate().atTime(time).atZone(now.zone)
        if (!candidate.isAfter(now)) {
            candidate = candidate.plusDays(1)
        }
        return candidate.toInstant().toEpochMilli()
    }

    private fun nextMatchingDay(
        now: ZonedDateTime,
        targetDays: Set<Int>,
        time: LocalTime
    ): Long? {
        if (targetDays.isEmpty()) return null
        for (offset in 0..7) {
            val date = now.toLocalDate().plusDays(offset.toLong())
            if (date.dayOfWeek.value in targetDays) {
                val candidate = date.atTime(time).atZone(now.zone)
                if (candidate.isAfter(now)) {
                    return candidate.toInstant().toEpochMilli()
                }
            }
        }
        return null
    }

    private fun nextShiftTrigger(alarm: AlarmEntity, now: ZonedDateTime): Long? {
        val length = alarm.shiftLengthDays.coerceIn(1, MAX_SHIFT_DAYS)
        val today = now.toLocalDate()
        val start = parseShiftDate(alarm.shiftStartDate) ?: today
        val slots = parseShiftSlots(alarm.shiftSlots, length)
        if (slots.all { it == null }) return null

        val baseDate = if (today.isAfter(start)) today else start
        val baseIndex = floorMod(ChronoUnit.DAYS.between(start, baseDate), length)

        for (offset in 0..length) {
            val date = baseDate.plusDays(offset.toLong())
            val index = floorMod(baseIndex.toLong() + offset, length)
            val minutes = slots.getOrNull(index) ?: continue
            val candidate = date
                .atTime(minutes / 60, minutes % 60)
                .atZone(now.zone)
            if (candidate.isAfter(now)) {
                return candidate.toInstant().toEpochMilli()
            }
        }
        return null
    }

    fun formatTimeUntil(
        context: Context,
        targetTime: Long?,
        nowMillis: Long = System.currentTimeMillis()
    ): String {
        if (targetTime == null) return context.getString(R.string.alarm_no_upcoming)
        val diff = targetTime - nowMillis
        if (diff <= 0) return context.getString(R.string.alarm_set_soon)

        val days = diff / (1000 * 60 * 60 * 24)
        val hours = (diff / (1000 * 60 * 60)) % 24
        val minutes = (diff / (1000 * 60)) % 60

        val parts = mutableListOf<String>()
        if (days > 0) {
            parts.add(
                context.resources.getQuantityString(
                    R.plurals.duration_days,
                    days.toInt(),
                    days.toInt()
                )
            )
        }
        if (hours > 0) {
            parts.add(
                context.resources.getQuantityString(
                    R.plurals.duration_hours,
                    hours.toInt(),
                    hours.toInt()
                )
            )
        }
        if (minutes > 0) {
            parts.add(
                context.resources.getQuantityString(
                    R.plurals.duration_minutes,
                    minutes.toInt(),
                    minutes.toInt()
                )
            )
        }

        if (parts.isEmpty()) return context.getString(R.string.alarm_set_soon)
        return context.getString(R.string.alarm_set_for, parts.joinToString(", "))
    }

    fun formatTimeUntil(
        targetTime: Long?,
        nowMillis: Long = System.currentTimeMillis()
    ): String {
        if (targetTime == null) return "No upcoming alarm"
        val diff = targetTime - nowMillis
        if (diff <= 0) return "Alarm set for less than a minute from now"

        val days = diff / (1000 * 60 * 60 * 24)
        val hours = (diff / (1000 * 60 * 60)) % 24
        val minutes = (diff / (1000 * 60)) % 60

        val parts = mutableListOf<String>()
        if (days > 0) parts.add("$days ${if (days == 1L) "day" else "days"}")
        if (hours > 0) parts.add("$hours ${if (hours == 1L) "hour" else "hours"}")
        if (minutes > 0) parts.add("$minutes ${if (minutes == 1L) "minute" else "minutes"}")

        if (parts.isEmpty()) return "Alarm set for less than a minute from now"
        return "Alarm set for " + parts.joinToString(", ") + " from now"
    }

    fun parseShiftDate(value: String): LocalDate? {
        if (value.isBlank()) return null
        return runCatching { LocalDate.parse(value) }.getOrNull()
    }

    /**
     * Parses a CSV of N minute-of-day values. "-1" (or any invalid value)
     * means that the day is disabled. Missing values are also disabled.
     */
    fun parseShiftSlots(slots: String, length: Int): List<Int?> {
        val safeLength = length.coerceIn(1, MAX_SHIFT_DAYS)
        val parts = if (slots.isBlank()) emptyList() else slots.split(",")
        return List(safeLength) { index ->
            val value = parts.getOrNull(index)?.trim()?.toIntOrNull()
            if (value != null && value in 0..1439) value else null
        }
    }

    fun encodeShiftSlots(values: List<Int?>): String {
        return values.joinToString(",") { it?.toString() ?: "-1" }
    }

    fun encodeShiftSlot(enabled: Boolean, hour: Int, minute: Int): Int? {
        if (!enabled) return null
        val time = safeTime(hour, minute)
        return time.hour * 60 + time.minute
    }

    fun decodeShiftSlot(value: Int?): Pair<Boolean, LocalTime> {
        if (value == null || value !in 0..1439) {
            return false to LocalTime.of(7, 0)
        }
        return true to LocalTime.of(value / 60, value % 60)
    }

    fun shiftLength(alarm: AlarmEntity): Int {
        return alarm.shiftLengthDays.coerceIn(1, MAX_SHIFT_DAYS)
    }

    /** Two alarms are duplicates when their time and repeat schedule match. */
    fun isSameSchedule(a: AlarmEntity, b: AlarmEntity): Boolean {
        if (a.timeHour != b.timeHour || a.timeMinute != b.timeMinute) return false
        if (a.daysOfWeek != b.daysOfWeek) return false
        if (a.shiftEnabled != b.shiftEnabled) return false
        if (!a.shiftEnabled) return true
        return a.shiftLengthDays == b.shiftLengthDays &&
            a.shiftStartDate == b.shiftStartDate &&
            a.shiftSlots == b.shiftSlots
    }

    fun zone(): ZoneId = ZoneId.systemDefault()

    private fun safeTime(hour: Int, minute: Int): LocalTime {
        return LocalTime.of(hour.coerceIn(0, 23), minute.coerceIn(0, 59))
    }

    private fun parseDays(value: String): Set<Int> {
        return value.split(",")
            .mapNotNull { dayNameToIso[it.trim()] }
            .toSet()
    }

    private fun floorMod(value: Long, modulus: Int): Int {
        val m = modulus.toLong()
        return (((value % m) + m) % m).toInt()
    }
}
