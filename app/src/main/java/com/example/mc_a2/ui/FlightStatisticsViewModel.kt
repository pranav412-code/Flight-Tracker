package com.example.mc_a2.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mc_a2.data.FlightRepository
import com.example.mc_a2.data.db.FlightDatabase
import com.example.mc_a2.data.db.FlightRecord
import com.example.mc_a2.data.db.RouteInfo
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
                updateRouteStatistics()
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
                        
                        // Then, schedule the periodic data collection with initial delay of 8 hours
                        flightDataManager.scheduleFlightDataCollection(flightsToTrack, 8)
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
    
    /**
     * Compute average flight time for all monitored routes
     */
    private suspend fun updateRouteStatistics() {
        viewModelScope.launch {
            val routes = repository.getAllUniqueRoutes()
            val stats = mutableListOf<RouteStatistic>()
            
            for (route in routes) {
                val avgTime = repository.getAverageFlightTimeForRoute(
                    route.departureAirport,
                    route.arrivalAirport
                )
                
                val flightCount = repository.getFlightCountForRoute(
                    route.departureAirport,
                    route.arrivalAirport
                )
                
                if (avgTime != null && flightCount > 0) {
                    val formattedTime = repository.formatAverageTime(avgTime)
                    
                    // Find departure and arrival city names from records
                    val record = _flightRecords.value.firstOrNull { 
                        it.departureAirport == route.departureAirport && 
                        it.arrivalAirport == route.arrivalAirport 
                    }
                    
                    val departureName = record?.departureCity ?: route.departureAirport
                    val arrivalName = record?.arrivalCity ?: route.arrivalAirport
                    
                    stats.add(
                        RouteStatistic(
                            departureAirport = route.departureAirport,
                            departureName = departureName,
                            arrivalAirport = route.arrivalAirport,
                            arrivalName = arrivalName,
                            averageTime = formattedTime,
                            averageTimeMillis = avgTime,
                            flightCount = flightCount
                        )
                    )
                }
            }
            
            // Sort by flight count (most monitored routes first)
            _routeStatistics.value = stats.sortedByDescending { it.flightCount }
        }
    }
}

/**
 * Data class to represent statistics for a flight route
 */
data class RouteStatistic(
    val departureAirport: String,
    val departureName: String,
    val arrivalAirport: String,
    val arrivalName: String,
    val averageTime: String,
    val averageTimeMillis: Long,
    val flightCount: Int
)