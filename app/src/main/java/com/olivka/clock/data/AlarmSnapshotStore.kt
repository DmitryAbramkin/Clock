package com.olivka.clock.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Small synchronous cache of the last alarm list. It lets the UI show the
 * previous state immediately after process death instead of flashing an
 * empty list while Room opens.
 */
class AlarmSnapshotStore(context: Context) {
    private val prefs = context.getSharedPreferences("alarm_snapshot", Context.MODE_PRIVATE)

    fun load(): List<AlarmEntity> {
        val raw = prefs.getString("alarms", null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val o = array.getJSONObject(i)
                    add(
                        AlarmEntity(
                            id = o.optInt("id", 0),
                            timeHour = o.optInt("timeHour", 0),
                            timeMinute = o.optInt("timeMinute", 0),
                            daysOfWeek = o.optString("daysOfWeek", "Never"),
                            isActive = o.optBoolean("isActive", false),
                            soundName = o.optString("soundName", "Default"),
                            soundUri = o.optString("soundUri", ""),
                            vibrationEnabled = o.optBoolean("vibrationEnabled", true),
                            vibrationFollowsGlobal = o.optBoolean(
                                "vibrationFollowsGlobal",
                                false
                            ),
                            shiftEnabled = o.optBoolean("shiftEnabled", false),
                            shiftLengthDays = o.optInt("shiftLengthDays", 4),
                            shiftStartDate = o.optString("shiftStartDate", ""),
                            shiftSlots = o.optString("shiftSlots", ""),
                            skipNextTriggerAt = o.optLong("skipNextTriggerAt", 0L),
                            label = o.optString("label", "")
                        )
                    )
                }
            }
        }.getOrElse { emptyList() }
    }

    fun save(alarms: List<AlarmEntity>) {
        val array = JSONArray()
        alarms.forEach { alarm ->
            array.put(
                JSONObject().apply {
                    put("id", alarm.id)
                    put("timeHour", alarm.timeHour)
                    put("timeMinute", alarm.timeMinute)
                    put("daysOfWeek", alarm.daysOfWeek)
                    put("isActive", alarm.isActive)
                    put("soundName", alarm.soundName)
                    put("soundUri", alarm.soundUri)
                    put("vibrationEnabled", alarm.vibrationEnabled)
                    put("vibrationFollowsGlobal", alarm.vibrationFollowsGlobal)
                    put("shiftEnabled", alarm.shiftEnabled)
                    put("shiftLengthDays", alarm.shiftLengthDays)
                    put("shiftStartDate", alarm.shiftStartDate)
                    put("shiftSlots", alarm.shiftSlots)
                    put("skipNextTriggerAt", alarm.skipNextTriggerAt)
                    put("label", alarm.label)
                }
            )
        }
        prefs.edit().putString("alarms", array.toString()).apply()
    }
}
