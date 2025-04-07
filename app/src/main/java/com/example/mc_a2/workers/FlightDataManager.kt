package com.example.mc_a2.workers

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Helper class to manage background flight data collection jobs
 */
class FlightDataManager(private val context: Context) {
    private val workManager = WorkManager.getInstance(context)
    
    /**
     * Schedule a daily job to collect data for the specified flights
     * Collects data for at least 3 flights per day
     * 
     * @param flightNumbers List of flight numbers to monitor
     */
    fun scheduleFlightDataCollection(flightNumbers: List<String>) {
        // Require network connection for data collection
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        // Create input data with flight numbers
        val inputData = Data.Builder()
            .putString(FlightDataCollectionWorker.KEY_FLIGHT_NUMBERS, flightNumbers.joinToString(","))
            .build()
        
        // Create work request to run 3 times per day (every 8 hours)
        val workRequest = PeriodicWorkRequestBuilder<FlightDataCollectionWorker>(
            8, TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .setInputData(inputData)
            .build()
        
        // Enqueue the work request
        workManager.enqueueUniquePeriodicWork(
            "flight_data_collection",
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }
    
    /**
     * One-time job to collect data immediately
     */
    fun collectFlightDataNow(flightNumbers: List<String>) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        val inputData = Data.Builder()
            .putString(FlightDataCollectionWorker.KEY_FLIGHT_NUMBERS, flightNumbers.joinToString(","))
            .build()
        
        val workRequest = androidx.work.OneTimeWorkRequestBuilder<FlightDataCollectionWorker>()
            .setConstraints(constraints)
            .setInputData(inputData)
            .build()
        
        workManager.enqueue(workRequest)
    }
    
    /**
     * Cancel all scheduled flight data collection jobs
     */
    fun cancelAllFlightDataCollection() {
        workManager.cancelUniqueWork("flight_data_collection")
    }
    
    /**
     * Check if flight data collection is scheduled
     */
    fun isFlightDataCollectionScheduled(): Boolean {
        val workInfos = workManager.getWorkInfosForUniqueWork("flight_data_collection").get()
        return workInfos.isNotEmpty() && workInfos.any { !it.state.isFinished }
    }
    
    companion object {
        // No predefined flight sample data as we only use manually tracked flights
    }
}