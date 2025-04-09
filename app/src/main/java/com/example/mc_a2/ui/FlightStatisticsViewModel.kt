package com.example.mc_a2.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mc_a2.data.FlightRepository
import com.example.mc_a2.data.db.FlightDatabase
import com.example.mc_a2.data.db.FlightRecord
import com.example.mc_a2.data.db.RouteInfo
import com.example.mc_a2.data.db.RouteStatisticEntity
import com.example.mc_a2.workers.FlightDataManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class FlightStatisticsViewModel(application: Application) : AndroidViewModel(application) {
    private val database = FlightDatabase.getDatabase(application)
    private val repository = FlightRepository(database)
    private val flightDataManager = FlightDataManager(application)
    
    // State for flight records
    private val _flightRecords = MutableStateFlow<List<FlightRecord>>(emptyList())
    val flightRecords: StateFlow<List<FlightRecord>> = _flightRecords
    
    // State for route statistics
    private val _routeStatistics = MutableStateFlow<List<RouteStatistic>>(emptyList())
    val routeStatistics: StateFlow<List<RouteStatistic>> = _routeStatistics
    
    // State for data collection status
    private val _dataCollectionActive = MutableStateFlow(false)
    val dataCollectionActive: StateFlow<Boolean> = _dataCollectionActive
    
    init {
        // Start monitoring flight records
        viewModelScope.launch {
            repository.getAllFlightRecords().collectLatest { records ->
                _flightRecords.value = records
            }
        }
        
        // Start monitoring route statistics from the new table
        viewModelScope.launch {
            repository.getAllRouteStatistics().collectLatest { routeStats ->
                // Convert from RouteStatisticEntity to RouteStatistic
                _routeStatistics.value = routeStats.map { entity ->
                    RouteStatistic(
                        departureAirport = entity.departureAirport,
                        departureName = entity.departureCity,
                        arrivalAirport = entity.arrivalAirport,
                        arrivalName = entity.arrivalCity,
                        averageTime = repository.formatAverageTime(entity.averageFlightTimeMinutes),
                        averageTimeMinutes = entity.averageFlightTimeMinutes,
                        flightCount = entity.flightCount
                    )
                }.sortedByDescending { it.flightCount }
            }
        }
        
        // Check the actual state of the WorkManager
        _dataCollectionActive.value = flightDataManager.isFlightDataCollectionScheduled()
    }
    
    /**
    * Start collecting flight data for tracked flights
    */
    fun startFlightDataCollection() {
        // Set the UI state immediately to prevent flickering
        _dataCollectionActive.value = true
        
        viewModelScope.launch {
            try {
                // First cancel any existing work to ensure a clean start
                flightDataManager.cancelAllFlightDataCollection()
                
                // Get all tracked flight numbers from the database WITHOUT using collectLatest
                // This avoids the recursive loop that was causing multiple API calls
                val records = repository.getAllFlightRecords().first()
                val trackedFlights = records.map { it.flightNumber }.distinct()
                
                if (trackedFlights.isNotEmpty()) {
                    // Only use the latest flight for data collection (to minimize API calls)
                    val latestFlight = trackedFlights.lastOrNull()
                    if (latestFlight != null) {
                        val flightsToTrack = listOf(latestFlight)
                        
                        // Reset the data collection counters to ensure immediate collection
                        flightDataManager.resetDataCollection()
                        
                        // First, trigger an immediate data collection
                        flightDataManager.collectFlightDataNow(flightsToTrack)
                        
                        // Then, schedule the periodic data collection with initial delay of 15 minutes
                        flightDataManager.scheduleFlightDataCollection(flightsToTrack, 15)
                    }
                } else {
                    // No flights have been tracked yet
                    _dataCollectionActive.value = false
                }
            } catch (e: Exception) {
                Log.e("FlightStatisticsVM", "Error starting flight data collection", e)
                _dataCollectionActive.value = false
            }
        }
    }
    
    /**
    * Stop flight data collection immediately
    */
    fun stopFlightDataCollection() {
        // Set the UI state immediately to prevent flickering
        _dataCollectionActive.value = false
        
        // Then cancel the work in the background
        viewModelScope.launch {
            flightDataManager.cancelAllFlightDataCollection()
        }
    }
}

/**
 * Data class to represent statistics for a flight route
 * This is a UI model separate from the database entity
 */
data class RouteStatistic(
    val departureAirport: String,
    val departureName: String,
    val arrivalAirport: String,
    val arrivalName: String,
    val averageTime: String,
    val averageTimeMinutes: Int, // Changed from averageTimeMillis to averageTimeMinutes
    val flightCount: Int
)