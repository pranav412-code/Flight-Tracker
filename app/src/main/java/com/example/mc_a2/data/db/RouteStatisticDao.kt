package com.example.mc_a2.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RouteStatisticDao {
    
    /**
     * Inserts or updates a route statistic
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateRouteStatistic(routeStatistic: RouteStatisticEntity)
    
    /**
     * Gets all route statistics
     */
    @Query("SELECT * FROM route_statistics ORDER BY flightCount DESC")
    fun getAllRouteStatistics(): Flow<List<RouteStatisticEntity>>
    
    /**
     * Gets a specific route statistic by departure and arrival airports
     */
    @Query("""
        SELECT * FROM route_statistics 
        WHERE departureAirport = :departureAirport AND arrivalAirport = :arrivalAirport
        LIMIT 1
    """)
    suspend fun getRouteStatistic(departureAirport: String, arrivalAirport: String): RouteStatisticEntity?
    
    /**
     * Deletes route statistics older than the specified date
     */
    @Query("DELETE FROM route_statistics WHERE lastUpdated < :olderThan")
    suspend fun deleteOldRouteStatistics(olderThan: Long)
    
    /**
     * Updates the flight count and average flight time for a route
     */
    @Query("""
        UPDATE route_statistics 
        SET flightCount = :flightCount,
            averageFlightTimeMinutes = :averageTimeMinutes,
            lastUpdated = :lastUpdated
        WHERE departureAirport = :departureAirport AND arrivalAirport = :arrivalAirport
    """)
    suspend fun updateRouteStatistics(
        departureAirport: String,
        arrivalAirport: String,
        flightCount: Int,
        averageTimeMinutes: Int,
        lastUpdated: Long = System.currentTimeMillis()
    )
}