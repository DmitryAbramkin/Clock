package com.olivka.clock

import android.app.Application
import com.olivka.clock.data.AlarmRepository
import com.olivka.clock.data.AlarmSnapshotStore
import com.olivka.clock.data.AppDatabase
import com.olivka.clock.data.TimerRepository
import com.olivka.clock.service.AlarmScheduler

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.olivka.clock.data.UserPreferencesRepository
import com.olivka.clock.ui.theme.ThemeManager

class StrictClockApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    private val alarmSnapshotStore by lazy { AlarmSnapshotStore(this) }
    val repository by lazy { AlarmRepository(database.alarmDao(), alarmSnapshotStore) }
    val timerRepository by lazy { TimerRepository(database.timerDao()) }
    val scheduler by lazy { AlarmScheduler(this) }
    val userPreferencesRepository by lazy { UserPreferencesRepository(this) }
    
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        ThemeManager.initialize(userPreferencesRepository, applicationScope)

        // Safety net: restore exact alarms whenever the process starts. This
        // covers cases where the boot/timezone broadcast was missed or the
        // credential-encrypted database was not available yet.
        ioScope.launch {
            try {
                repository.allAlarms.first().forEach { alarm ->
                    if (alarm.isActive) scheduler.schedule(alarm)
                }
                val now = System.currentTimeMillis()
                timerRepository.allTimers.first().forEach { timer ->
                    if (timer.isRunning) {
                        val remaining = timer.endAtMillis - now
                        if (remaining > 0L) {
                            scheduler.scheduleTimer(
                                timer.id,
                                remaining,
                                timer.label,
                                timer.soundUri,
                                timer.vibrationEnabled,
                                timer.vibrationFollowsGlobal
                            )
                        }
                    }
                }
            } catch (_: Exception) {
                // Database is not available yet (e.g. before user unlock).
            }
        }
    }
}
