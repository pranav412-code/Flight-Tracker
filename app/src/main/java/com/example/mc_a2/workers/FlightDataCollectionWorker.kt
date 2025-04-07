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
        
        // Input data keys
        const val KEY_FLIGHT_NUMBERS = "FLIGHT_NUMBERS"
        
        // Preference keys
        private const val PREF_DEPARTURE_AIRPORT = "departure_airport"
        private const val PREF_ARRIVAL_AIRPORT = "arrival_airport"
        private const val PREF_DATA_COLLECTION_COUNT = "data_collection_count"
        private const val PREF_LAST_COLLECTION_TIME = "last_collection_time"
        
        // Collection parameters
        private const val MAX_DATA_COLLECTION_COUNT = 10  // Maximum number of successful collections
        private const val MIN_COLLECTION_INTERVAL_HOURS = 2  // Minimum time between collections in hours
    }
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting flight data collection")
            
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
                Log.d(TAG, "Not enough time elapsed since last collection. Need to wait at least $MIN_COLLECTION_INTERVAL_HOURS hours.")
                return@withContext Result.success()  // Return success to avoid immediate retry
            }
            
            // Get flight numbers from input data
            val flightNumbersString = inputData.getString(KEY_FLIGHT_NUMBERS) ?: ""
            if (flightNumbersString.isEmpty()) {
                Log.e(TAG, "No flight numbers provided")
                return@withContext Result.failure()
            }
            
            // Parse flight numbers (comma-separated list)
            val flightNumbers = flightNumbersString.split(",").map { it.trim() }
            
            // Only process the first flight in the list to minimize API calls
            val flightNumber = flightNumbers.firstOrNull()
            if (flightNumber == null) {
                Log.e(TAG, "No valid flight number found")
                return@withContext Result.failure()
            }
            
            // Initialize repository
            val database = FlightDatabase.getDatabase(applicationContext)
            val repository = FlightRepository(database)
            
            // Check if we already have a saved route from previous runs
            val savedDepartureAirport = prefs.getString(PREF_DEPARTURE_AIRPORT, null)
            val savedArrivalAirport = prefs.getString(PREF_ARRIVAL_AIRPORT, null)
            
            // Variables to track the flight's route
            var departureAirport = savedDepartureAirport
            var arrivalAirport = savedArrivalAirport
            
            try {
                Log.d(TAG, "Fetching data for flight: $flightNumber")
                val result = repository.fetchAndStoreFlightData(flightNumber)
                
                if (result is com.example.mc_a2.data.Result.Success) {
                    val flight = result.data
                    
                    // If this is a successful flight and we don't have saved route info,
                    // save the origin and destination
                    if ((departureAirport == null || arrivalAirport == null) && flight != null) {
                        flight.departure?.iata?.let { dept ->
                            flight.arrival?.iata?.let { arr ->
                                departureAirport = dept
                                arrivalAirport = arr
                                
                                // Save the route information for future runs
                                prefs.edit()
                                    .putString(PREF_DEPARTURE_AIRPORT, dept)
                                    .putString(PREF_ARRIVAL_AIRPORT, arr)
                                    .apply()
                                
                                Log.d(TAG, "Saved route information: $dept to $arr")
                            }
                        }
                    }
                    
                    // Check if this flight matches our target route
                    val matchesRoute = flight != null && 
                                        departureAirport != null && 
                                        arrivalAirport != null && 
                                        flight.departure?.iata == departureAirport && 
                                        flight.arrival?.iata == arrivalAirport
                    
                    if (matchesRoute) {
                        Log.d(TAG, "Successfully stored data for flight: $flightNumber on route $departureAirport to $arrivalAirport")
                        
                        // Update collection counter and last collection time
                        prefs.edit()
                            .putInt(PREF_DATA_COLLECTION_COUNT, currentCollectionCount + 1)
                            .putLong(PREF_LAST_COLLECTION_TIME, currentTime)
                            .apply()
                        
                        Log.d(TAG, "Updated collection count: ${currentCollectionCount + 1}/$MAX_DATA_COLLECTION_COUNT")
                        
                        // Clean up old records
                        repository.cleanUpOldRecords()
                        return@withContext Result.success()
                    } else if (departureAirport != null && arrivalAirport != null) {
                        Log.d(TAG, "Flight $flightNumber doesn't match our target route $departureAirport to $arrivalAirport")
                    }
                } else if (result is com.example.mc_a2.data.Result.Error) {
                    Log.e(TAG, "Error fetching data for flight: $flightNumber - ${result.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception during flight data collection", e)
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