package com.example.data

import kotlinx.coroutines.flow.Flow

class EvidenceRepository(private val dao: RecorderDao) {
    val allIncidents: Flow<List<IncidentRecord>> = dao.getAllIncidents()
    val uniqueTrips: Flow<List<String>> = dao.getUniqueTrips()

    suspend fun getIncidentById(id: Long): IncidentRecord? = dao.getIncidentById(id)

    suspend fun insertIncident(incident: IncidentRecord): Long = dao.insertIncident(incident)

    suspend fun deleteIncident(id: Long) = dao.deleteIncidentById(id)

    suspend fun clearAllIncidents() = dao.clearAllIncidents()

    fun getPointsForTrip(runId: String): Flow<List<TripPoint>> = dao.getPointsForTrip(runId)

    suspend fun insertTripPoint(point: TripPoint) = dao.insertTripPoint(point)

    suspend fun getRecentTripPoints(): List<TripPoint> = dao.getRecentTripPoints()

    suspend fun deleteTrip(runId: String) = dao.deleteTrip(runId)
}
