package com.olivka.clock.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A single countdown timer. [remainingMs] is the last committed value; while
 * the timer is running [endAtMillis] holds the wall-clock end of the countdown
 * so the state survives process death and can be restored from an exact alarm.
 */
@Entity(tableName = "timers")
data class TimerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val label: String = "",
    val durationMs: Long,
    val remainingMs: Long,
    val isRunning: Boolean = false,
    val endAtMillis: Long = 0L,
    val createdAt: Long = 0L,
    /**
     * True once the timer has been started and not reset since. Paused timers
     * keep their progress line, while a freshly created or reset timer does
     * not.
     */
    val hasStarted: Boolean = false,
    val soundName: String = "Default",
    val soundUri: String = "",
    val vibrationEnabled: Boolean = true,
    val vibrationFollowsGlobal: Boolean = true
)
