package com.olivka.clock.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val timeHour: Int,
    val timeMinute: Int,
    val daysOfWeek: String,
    val isActive: Boolean,
    val soundName: String = "Default",
    val soundUri: String = "",
    val vibrationEnabled: Boolean = true,
    val vibrationFollowsGlobal: Boolean = true,
    val shiftEnabled: Boolean = false,
    val shiftLengthDays: Int = 4,
    val shiftStartDate: String = "",
    val shiftSlots: String = "",
    val skipNextTriggerAt: Long = 0L,
    val label: String = ""
)
