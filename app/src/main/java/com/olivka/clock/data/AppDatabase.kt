package com.olivka.clock.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [AlarmEntity::class, TimerEntity::class],
    version = 13,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun alarmDao(): AlarmDao
    abstract fun timerDao(): TimerDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE alarms ADD COLUMN skipNextTriggerAt INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE alarms ADD COLUMN label TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS timers (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "label TEXT NOT NULL DEFAULT '', " +
                        "durationMs INTEGER NOT NULL DEFAULT 0, " +
                        "remainingMs INTEGER NOT NULL DEFAULT 0, " +
                        "isRunning INTEGER NOT NULL DEFAULT 0, " +
                        "endAtMillis INTEGER NOT NULL DEFAULT 0, " +
                        "createdAt INTEGER NOT NULL DEFAULT 0)"
                )
            }
        }

        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE timers ADD COLUMN hasStarted INTEGER NOT NULL DEFAULT 0"
                )
                // Existing timers that are running, paused part-way or already
                // finished were started at least once. Only freshly created
                // and reset timers keep hasStarted = 0.
                db.execSQL(
                    "UPDATE timers SET hasStarted = 1 " +
                        "WHERE isRunning = 1 OR remainingMs < durationMs OR remainingMs = 0"
                )
            }
        }

        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE timers ADD COLUMN soundName TEXT NOT NULL DEFAULT 'Default'"
                )
                db.execSQL(
                    "ALTER TABLE timers ADD COLUMN soundUri TEXT NOT NULL DEFAULT ''"
                )
                db.execSQL(
                    "ALTER TABLE timers ADD COLUMN vibrationEnabled INTEGER NOT NULL DEFAULT 1"
                )
            }
        }

        private val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Existing alarms/timers already have explicit vibration
                // values, so they stay as concrete overrides. New items are
                // created with a link to the global vibration setting.
                db.execSQL(
                    "ALTER TABLE alarms ADD COLUMN vibrationFollowsGlobal INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "ALTER TABLE timers ADD COLUMN vibrationFollowsGlobal INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "strict_clock_database"
                )
                .addMigrations(
                    MIGRATION_7_8,
                    MIGRATION_8_9,
                    MIGRATION_9_10,
                    MIGRATION_10_11,
                    MIGRATION_11_12,
                    MIGRATION_12_13
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
