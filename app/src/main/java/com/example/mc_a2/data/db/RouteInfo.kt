package com.example.mc_a2.data.db

import androidx.room.ColumnInfo

/**
 * Data class to hold unique route information
 */
data class RouteInfo(
    @ColumnInfo(name = "departureAirport") val departureAirport: String,
    @ColumnInfo(name = "arrivalAirport") val arrivalAirport: String
)