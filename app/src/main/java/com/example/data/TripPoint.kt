package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trip_points")
data class TripPoint(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val runId: String, // Groups points by trip session
    val timestamp: Long = System.currentTimeMillis(),
    val latitude: Double,
    val longitude: Double,
    val speedMph: Double,
    val streetName: String = "",
    val countyName: String = ""
)
