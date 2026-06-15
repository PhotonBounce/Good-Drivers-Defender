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
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
