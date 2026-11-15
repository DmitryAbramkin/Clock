package com.olivka.clock.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach

class AlarmRepository(
    private val alarmDao: AlarmDao,
    private val snapshotStore: AlarmSnapshotStore
) {
    @Volatile
    private var lastAlarms: List<AlarmEntity> = snapshotStore.load()
    val cachedAlarms: List<AlarmEntity> get() = lastAlarms

    val allAlarms: Flow<List<AlarmEntity>> = alarmDao.getAllAlarms()
        .onEach {
            lastAlarms = it
            snapshotStore.save(it)
        }

    suspend fun getAlarmById(id: Int): AlarmEntity? {
        return alarmDao.getAlarmById(id)
    }

    suspend fun insert(alarm: AlarmEntity): Long {
        return alarmDao.insertAlarm(alarm)
    }

    suspend fun update(alarm: AlarmEntity) {
        alarmDao.updateAlarm(alarm)
    }

    suspend fun delete(alarm: AlarmEntity) {
        alarmDao.deleteAlarm(alarm)
    }
}
