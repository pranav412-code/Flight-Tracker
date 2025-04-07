package com.example.mc_a2.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mc_a2.data.FlightRepository
import com.example.mc_a2.data.Result
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

class FlightTrackingViewModel : ViewModel() {
    private val repository = FlightRepository()
    
    private val _uiState = MutableStateFlow<FlightTrackingState>(FlightTrackingState.Initial)
    val uiState: StateFlow<FlightTrackingState> = _uiState
    
    private val _lastFetchTime = MutableStateFlow<String?>(null)
    val lastFetchTime: StateFlow<String?> = _lastFetchTime
    
    private var trackingTimer: Timer? = null
    
    fun trackFlight(flightNumber: String) {
        if (flightNumber.isBlank()) {
            _uiState.value = FlightTrackingState.Error("Please enter a valid flight number")
            return
        }
        
        _uiState.value = FlightTrackingState.Loading
        
        // Cancel any existing timer
        stopTracking()
        
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
                    _uiState.value = FlightTrackingState.Error("Error: ${e.message}")
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
                            } else {
                                _uiState.value = FlightTrackingState.Error("Flight not found")
                                stopTracking()
                            }
                        }
                        is Result.Error -> {
                            _uiState.value = FlightTrackingState.Error(result.message)
                            stopTracking()
                        }
                    }
                }
        }
    }
    
    fun stopTracking() {
        trackingTimer?.cancel()
        trackingTimer = null
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