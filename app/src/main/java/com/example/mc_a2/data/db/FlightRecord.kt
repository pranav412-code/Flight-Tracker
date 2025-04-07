package com.example.mc_a2.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Entity for storing historical flight data
 * This will be used to calculate average flight times
 */
@Entity(tableName = "flight_records")
data class FlightRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    // Flight identification
    val flightNumber: String,
    val airline: String,
    
    // Route information
    val departureAirport: String,
    val departureCity: String,
    val arrivalAirport: String,
    val arrivalCity: String,
    
    // Time information
    val scheduledDepartureTime: Long, // Stored as milliseconds since epoch
    val actualDepartureTime: Long?,
    val scheduledArrivalTime: Long,
    val actualArrivalTime: Long?,
    
    // Delay information
    val departureDelayMinutes: Int?,
    val arrivalDelayMinutes: Int?,
    
    // Metadata
    val recordDate: Long = System.currentTimeMillis(), // When we collected this data
    val flightDate: String // The date of the flight (YYYY-MM-DD)
)