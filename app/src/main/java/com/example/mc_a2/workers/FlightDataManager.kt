package com.example.mc_a2.workers

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
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
     * @param initialDelayHours Number of hours to delay the first execution
     */
    fun scheduleFlightDataCollection(flightNumbers: List<String>, initialDelayHours: Long = 8) {
        // Check if periodic collection is already scheduled
        if (isFlightDataCollectionScheduled()) {
            Log.d("FlightDataManager", "Periodic flight data collection already scheduled, skipping")
            return
        }
        
        // Require network connection for data collection
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        // Create work request to run 3 times per day (every 8 hours)
        // With initial delay to avoid immediate execution
        val workRequest = PeriodicWorkRequestBuilder<FlightDataCollectionWorker>(
            8, TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .setInitialDelay(initialDelayHours, TimeUnit.HOURS) // Add initial delay
            .addTag(FlightDataCollectionWorker.WORK_NAME_PERIODIC)
            .build()
        
        // Enqueue the work request
        workManager.enqueueUniquePeriodicWork(
            FlightDataCollectionWorker.WORK_NAME_PERIODIC,
            ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE, // Replace any existing work
            workRequest
        )
        
        Log.d("FlightDataManager", "Scheduled periodic flight data collection")
    }
    
    /**
     * One-time job to collect data immediately
     */
    fun collectFlightDataNow(flightNumbers: List<String>) {
        // Check if a one-time collection is already in progress
        val workInfos = workManager.getWorkInfosForUniqueWork(FlightDataCollectionWorker.WORK_NAME_ONETIME).get()
        val hasActiveWork = workInfos.any { !it.state.isFinished && it.state != WorkInfo.State.CANCELLED }
        
        if (hasActiveWork) {
            Log.d("FlightDataManager", "One-time flight data collection already in progress, skipping")
            return
        }
        
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        val workRequest = OneTimeWorkRequestBuilder<FlightDataCollectionWorker>()
            .setConstraints(constraints)
            .addTag(FlightDataCollectionWorker.WORK_NAME_ONETIME)
            .build()
        
        // Use unique work to prevent multiple simultaneous one-time requests
        workManager.enqueueUniqueWork(
            FlightDataCollectionWorker.WORK_NAME_ONETIME,
            ExistingWorkPolicy.REPLACE, // Replace any existing one-time work
            workRequest
        )
        
        Log.d("FlightDataManager", "Scheduled one-time flight data collection")
    }
    
    /**
     * Cancel all scheduled flight data collection jobs
     */
    fun cancelAllFlightDataCollection() {
        // Cancel periodic work
        workManager.cancelUniqueWork(FlightDataCollectionWorker.WORK_NAME_PERIODIC)
        
        // Cancel any one-time work
        workManager.cancelUniqueWork(FlightDataCollectionWorker.WORK_NAME_ONETIME)
        
        // Prune work to make sure everything is cleaned up
        workManager.pruneWork()
        
        Log.d("FlightDataManager", "Canceled all flight data collection jobs")
    }
    
    /**
     * Check if flight data collection is scheduled
     */
    fun isFlightDataCollectionScheduled(): Boolean {
        // Get work info for periodic work
        val workInfos = workManager.getWorkInfosForUniqueWork(
            FlightDataCollectionWorker.WORK_NAME_PERIODIC
        ).get()
        
        // Check if there's any active periodic work
        val isActive = workInfos.isNotEmpty() && 
            workInfos.any { !it.state.isFinished && it.state != WorkInfo.State.CANCELLED }
        
        Log.d("FlightDataManager", "Checking work status: isActive=$isActive")
        return isActive
    }
    
    /**
     * Resets the data collection counter to allow immediate data collection
     * without waiting for the MIN_COLLECTION_INTERVAL_HOURS
     */
    fun resetDataCollection() {
        val prefs = context.getSharedPreferences("flight_route_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putInt(FlightDataCollectionWorker.PREF_DATA_COLLECTION_COUNT, 0)
            .putLong(FlightDataCollectionWorker.PREF_LAST_COLLECTION_TIME, 0)
            .apply()
        
        Log.d("FlightDataManager", "Reset data collection counters")
    }
    
    companion object {
        // No predefined flight sample data as we only use manually tracked flights
    }
}