package com.example.mc_a2.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for the flight_records table
 */
@Dao
interface FlightRecordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlightRecord(flightRecord: FlightRecord): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlightRecords(flightRecords: List<FlightRecord>)
    
    @Query("SELECT * FROM flight_records ORDER BY recordDate DESC")
    fun getAllFlightRecords(): Flow<List<FlightRecord>>
    
    @Query("SELECT * FROM flight_records WHERE flightNumber = :flightNumber ORDER BY recordDate DESC LIMIT 1")
    suspend fun getLatestFlightRecord(flightNumber: String): FlightRecord?
    
    /**
     * Gets average flight time in milliseconds for a given route, accounting for delays
     */
    @Query("""
        SELECT AVG(
            CASE 
                WHEN actualArrivalTime IS NOT NULL AND actualDepartureTime IS NOT NULL 
                THEN actualArrivalTime - actualDepartureTime
                ELSE scheduledArrivalTime - scheduledDepartureTime + IFNULL(arrivalDelayMinutes, 0) * 60 * 1000
            END
        ) as avgTime
        FROM flight_records
        WHERE departureAirport = :departureAirport AND arrivalAirport = :arrivalAirport
        AND recordDate >= :startDate
    """)
    suspend fun getAverageFlightTimeForRoute(
        departureAirport: String,
        arrivalAirport: String,
        startDate: Long // Start date to filter records (e.g., 7 days ago)
    ): Long?
    
    /**
     * Gets the number of flights collected for a specific route since a given date
     */
    @Query("""
        SELECT COUNT(*) FROM flight_records
        WHERE departureAirport = :departureAirport AND arrivalAirport = :arrivalAirport
        AND recordDate >= :startDate
    """)
    suspend fun getFlightCountForRoute(
        departureAirport: String,
        arrivalAirport: String,
        startDate: Long
    ): Int
    
    /**
     * Gets all unique routes (departure-arrival airport pairs) in the database
     */
    @Query("""
        SELECT DISTINCT departureAirport, arrivalAirport FROM flight_records
    """)
    suspend fun getAllUniqueRoutes(): List<RouteInfo>
    
    /**
     * Deletes records older than the specified date
     */
    @Query("DELETE FROM flight_records WHERE recordDate < :olderThan")
    suspend fun deleteOldRecords(olderThan: Long)
}