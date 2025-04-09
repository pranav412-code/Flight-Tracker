package com.example.mc_a2.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mc_a2.data.FlightRepository
import com.example.mc_a2.data.Result
import com.example.mc_a2.data.db.FlightDatabase
import com.example.mc_a2.data.model.Flight
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Timer
import java.util.TimerTask
import android.util.Log

class FlightTrackingViewModel(application: Application) : AndroidViewModel(application) {
    private val database = FlightDatabase.getDatabase(application)
    private val repository = FlightRepository(database)
    
    private val _uiState = MutableStateFlow<FlightTrackingState>(FlightTrackingState.Initial)
    val uiState: StateFlow<FlightTrackingState> = _uiState
    
    private val _lastFetchTime = MutableStateFlow<String?>(null)
    val lastFetchTime: StateFlow<String?> = _lastFetchTime
    
    // Add a flag to track when tracking is stopped
    private val _isTrackingStopped = MutableStateFlow(false)
    val isTrackingStopped: StateFlow<Boolean> = _isTrackingStopped
    
    private var trackingTimer: Timer? = null
    private var currentFlightNumber: String? = null
    
    fun trackFlight(flightNumber: String) {
        if (flightNumber.isBlank()) {
            _uiState.value = FlightTrackingState.Error("Please enter a valid flight number")
            return
        }
        
        _uiState.value = FlightTrackingState.Loading
        
        // Reset tracking stopped flag
        _isTrackingStopped.value = false
        
        // Cancel any existing timer
        stopTracking(false)
        
        // Store the current flight number
        currentFlightNumber = flightNumber
        
        // Start fetching flight data immediately
        fetchFlightData(flightNumber)
        
        // Set up periodic updates every minute
        trackingTimer = Timer().apply {
            scheduleAtFixedRate(object : TimerTask() {
                override fun run() {
                    fetchFlightData(flightNumber)
                }
            }, 60000, 60000) // Update every minute after the first fetch
        }
    }
    
    private fun fetchFlightData(flightNumber: String) {
        viewModelScope.launch {
            // Update fetch time for every attempt
            _lastFetchTime.value = getCurrentTime()
            
            repository.getFlightByNumber(flightNumber)
                .catch { e ->
                    // Log the detailed error
                    Log.e("FlightTrackingViewModel", "API Error: ${e.message}")
                    // Show user-friendly message
                    _uiState.value = FlightTrackingState.Error("Network Error. Please try again later")
                }
                .collect { result -> 
                    when (result) {
                        is Result.Loading -> {
                            if (_uiState.value !is FlightTrackingState.Success) {
                                _uiState.value = FlightTrackingState.Loading
                            }
                        }
                        is Result.Success -> {
                            val flight = result.data
                            if (flight != null) {
                                _uiState.value = FlightTrackingState.Success(flight)
                                
                                // Store flight data in the database for statistics
                                saveFlight(flight)
                            } else {
                                _uiState.value = FlightTrackingState.Error("Flight not found")
                                stopTracking(true)
                            }
                        }
                        is Result.Error -> {
                            // Log the detailed error
                            Log.e("FlightTrackingViewModel", "API Error: ${result.message}")
                            // Show user-friendly message
                            _uiState.value = FlightTrackingState.Error("Network Error. Please try again later")
                            stopTracking(true)
                        }
                    }
                }
        }
    }
    
    private fun saveFlight(flight: Flight) {
        viewModelScope.launch {
            repository.saveFlight(flight)
        }
    }
    
    fun stopTracking(setStoppedFlag: Boolean = true) {
        trackingTimer?.cancel()
        trackingTimer = null
        currentFlightNumber = null
        
        if (setStoppedFlag) {
            _isTrackingStopped.value = true
        }
    }
    
    fun resetTrackingStatus() {
        if (_isTrackingStopped.value) {
            _isTrackingStopped.value = true
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        stopTracking()
    }
    
    private fun getCurrentTime(): String {
        val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault())
        return dateFormat.format(Date())
    }
}

sealed class FlightTrackingState {
    object Initial : FlightTrackingState()
    object Loading : FlightTrackingState()
    data class Success(val flight: Flight) : FlightTrackingState()
    data class Error(val message: String) : FlightTrackingState()
}