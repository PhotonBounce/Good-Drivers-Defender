package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RecorderDao {
    // Incidents
    @Query("SELECT * FROM incidents ORDER BY timestamp DESC")
    fun getAllIncidents(): Flow<List<IncidentRecord>>

    @Query("SELECT * FROM incidents WHERE id = :id LIMIT 1")
    suspend fun getIncidentById(id: Long): IncidentRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncident(incident: IncidentRecord): Long

    @Query("DELETE FROM incidents WHERE id = :id")
    suspend fun deleteIncidentById(id: Long)

    @Query("DELETE FROM incidents")
    suspend fun clearAllIncidents()

    // Trippoints
    @Query("SELECT * FROM trip_points WHERE runId = :runId ORDER BY timestamp ASC")
    fun getPointsForTrip(runId: String): Flow<List<TripPoint>>

    // One row per trip, ordered by each trip's most recent point. Using GROUP BY + MAX(timestamp)
    // (instead of DISTINCT + ORDER BY timestamp) makes "most recent trip first" deterministic —
    // with DISTINCT the order depended on an arbitrary per-trip timestamp.
    @Query("SELECT runId FROM trip_points GROUP BY runId ORDER BY MAX(timestamp) DESC")
    fun getUniqueTrips(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTripPoint(point: TripPoint)

    @Query("SELECT * FROM trip_points ORDER BY timestamp DESC LIMIT 500")
    suspend fun getRecentTripPoints(): List<TripPoint>

    @Query("DELETE FROM trip_points WHERE runId = :runId")
    suspend fun deleteTrip(runId: String)
}
