package com.example.mc_a2.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity for storing route statistics
 * This stores precomputed statistics for flight routes to optimize statistics display
 */
@Entity(tableName = "route_statistics")
data class RouteStatisticEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    // Route identification
    val departureAirport: String,
    val departureCity: String,
    val arrivalAirport: String,
    val arrivalCity: String,
    
    // Statistics - store time in minutes instead of milliseconds
    val averageFlightTimeMinutes: Int,
    val flightCount: Int,
    
    // Metadata
    val lastUpdated: Long = System.currentTimeMillis()
)