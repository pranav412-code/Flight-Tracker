package com.example.mc_a2.workers

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.mc_a2.data.FlightRepository
import com.example.mc_a2.data.Result
import com.example.mc_a2.data.db.FlightDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Worker to collect flight data for specific routes
 * This worker will fetch data for flights between specified airports
 */
class FlightDataCollectionWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {
    
    // SharedPreferences for storing route information
    private val prefs: SharedPreferences = applicationContext.getSharedPreferences(
        "flight_route_prefs", Context.MODE_PRIVATE
    )
    
    companion object {
        const val TAG = "FlightDataCollectionWorker"
        
        // Work tags for identification
        const val WORK_NAME_PERIODIC = "flight_data_collection"
        const val WORK_NAME_ONETIME = "flight_data_collection_once"
        
        // Preference keys - made public for use in FlightDataManager
        const val PREF_DATA_COLLECTION_COUNT = "data_collection_count"
        const val PREF_LAST_COLLECTION_TIME = "last_collection_time"
        
        // Collection parameters
        private const val MAX_DATA_COLLECTION_COUNT = 10  // Maximum number of successful collections
        private const val MIN_COLLECTION_INTERVAL_HOURS = 0  // Minimum time between collections in hours
    }
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting flight data collection")
            
            // For one-time work, skip the interval check
            val isOneTimeWork = this@FlightDataCollectionWorker.tags.contains(WORK_NAME_ONETIME)
            
            if (!isOneTimeWork) {
                // Check if we've already collected enough data
                val currentCollectionCount = prefs.getInt(PREF_DATA_COLLECTION_COUNT, 0)
                if (currentCollectionCount >= MAX_DATA_COLLECTION_COUNT) {
                    Log.d(TAG, "Maximum data collection count reached ($MAX_DATA_COLLECTION_COUNT). Stopping collection.")
                    return@withContext Result.success()
                }
                
                // Check if enough time has passed since last collection
                val lastCollectionTime = prefs.getLong(PREF_LAST_COLLECTION_TIME, 0)
                val currentTime = System.currentTimeMillis()
                val hoursElapsed = (currentTime - lastCollectionTime) / (1000 * 60 * 60)
                
                if (lastCollectionTime > 0 && hoursElapsed < MIN_COLLECTION_INTERVAL_HOURS) {
                    Log.d(TAG, "Not enough time elapsed since last collection (${hoursElapsed}h). Need to wait at least $MIN_COLLECTION_INTERVAL_HOURS hours.")
                    return@withContext Result.success()  // Return success to avoid immediate retry
                }
            } else {
                Log.d(TAG, "Executing one-time collection, skipping time interval check")
            }
            
            // Initialize repository
            val database = FlightDatabase.getDatabase(applicationContext)
            val repository = FlightRepository(database)
            
            // Fetch the last flight record from the database
            val lastRecord = repository.getLastFlightRecord()
            if (lastRecord == null) {
                Log.e(TAG, "No flight record found in database")
                return@withContext Result.failure()
            }
            val departureAirport = lastRecord.departureAirport
            val arrivalAirport = lastRecord.arrivalAirport
            
            Log.d(TAG, "Fetching flight data for route: $departureAirport -> $arrivalAirport")
            val result = repository.fetchAndStoreFlightDataByRoute(departureAirport, arrivalAirport)
            
            if (result is com.example.mc_a2.data.Result.Success) {
                Log.d(TAG, "Successfully stored flight data for route $departureAirport to $arrivalAirport")
                
                // Update collection counter and last collection time
                val currentCollectionCount = prefs.getInt(PREF_DATA_COLLECTION_COUNT, 0)
                prefs.edit()
                    .putInt(PREF_DATA_COLLECTION_COUNT, currentCollectionCount + 1)
                    .putLong(PREF_LAST_COLLECTION_TIME, System.currentTimeMillis())
                    .apply()
                
                Log.d(TAG, "Updated collection count: ${currentCollectionCount + 1}/$MAX_DATA_COLLECTION_COUNT")
                
                // Clean up old records
                repository.cleanUpOldRecords()
                return@withContext Result.success()
            }
            
            return@withContext Result.retry()
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error in worker", e)
            return@withContext Result.failure()
        }
    }
    
    /**
     * Resets the data collection counter to allow a new series of collections
     */
    fun resetDataCollection(context: Context) {
        val prefs = context.getSharedPreferences("flight_route_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putInt(PREF_DATA_COLLECTION_COUNT, 0)
            .putLong(PREF_LAST_COLLECTION_TIME, 0)
            .apply()
        
        Log.d(TAG, "Data collection counter reset. Will start collecting data again.")
    }
}