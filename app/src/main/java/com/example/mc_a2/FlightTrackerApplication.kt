package com.example.mc_a2

import android.app.Application
import androidx.work.Configuration
import com.example.mc_a2.workers.FlightDataManager

class FlightTrackerApplication : Application(), Configuration.Provider {
    
    private lateinit var flightDataManager: FlightDataManager
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize the flight data manager
        flightDataManager = FlightDataManager(this)
        
        // Removed automatic sample flight scheduling
        // We'll only track flights that the user explicitly requests
    }
    
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
}