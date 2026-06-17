package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [IncidentRecord::class, TripPoint::class], version = 1, exportSchema = false)
abstract class RecorderDatabase : RoomDatabase() {
    abstract fun recorderDao(): RecorderDao

    companion object {
        @Volatile
        private var INSTANCE: RecorderDatabase? = null

        fun getDatabase(context: Context): RecorderDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RecorderDatabase::class.java,
                    "driver_recorder_db"
                )
                    // ⚠️ DATA-LOSS LANDMINE — READ BEFORE BUMPING THE SCHEMA VERSION.
                    // This wipes ALL stored incidents/evidence whenever the schema changes.
                    // It is safe ONLY while version = 1 and the app has no released users with
                    // saved data. Before you add/rename a column (version 2+), you MUST:
                    //   1) set exportSchema = true and commit the generated schema JSON, and
                    //   2) add a proper Migration(1, 2) via .addMigrations(...),
                    // then REMOVE this call. Shipping a schema change with this still here will
                    // silently delete every user's evidence on update.
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
