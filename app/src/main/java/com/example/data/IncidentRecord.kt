package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "incidents")
data class IncidentRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val isAutoCaptured: Boolean = false,
    
    // Telemetry at event time
    val speedMph: Double,
    val speedLimitMph: Int,
    val maxGForce: Double, // Sudden braking or impact force
    
    // Geolocation details (important for incident reports)
    val latitude: Double,
    val longitude: Double,
    val streetOrHighway: String,
    val city: String,
    val county: String,
    val state: String = "US",
    
    // Defendant description + police complaint outline
    val defendantPlate: String = "",
    val defendantCarModelColor: String = "",
    val recklessBehaviorObserved: String = "", // e.g., "Tailgating", "Illegal U-Turn", "Speeding"
    val extraNotes: String = "",
    
    // Media attachment identifier (simulating localized captured camera buffers)
    val sessionFrameFolder: String = "",
    val frontCameraSupported: Boolean = false,
    val rearCameraSupported: Boolean = true
)
