package com.example.mc_a2.data

import com.example.mc_a2.data.api.FlightApiService
import com.example.mc_a2.data.api.NetworkModule
import com.example.mc_a2.data.db.FlightDatabase
import com.example.mc_a2.data.db.FlightRecord
import com.example.mc_a2.data.db.RouteInfo
import com.example.mc_a2.data.db.RouteStatisticEntity
import com.example.mc_a2.data.model.Flight
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first
import retrofit2.HttpException
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

class FlightRepository(private val database: FlightDatabase) {
    private val flightRecordDao = database.flightRecordDao()
    private val routeStatisticDao = database.routeStatisticDao()
    private val apiService = NetworkModule.flightApiService
    private val apiKey = NetworkModule.getApiKey()
    
    // Original method for fetching a single flight
    fun getFlightByNumber(flightNumber: String): Flow<Result<Flight?>> = flow {
        try {
            emit(Result.Loading)
            
            val response = apiService.getFlightByNumber(apiKey, flightNumber)
            if (response.isSuccessful) {
                val flightResponse = response.body()
                val flight = flightResponse?.data?.firstOrNull()
                emit(Result.Success(flight))
            } else {
                emit(Result.Error("API Error: ${response.message()}"))
            }
        } catch (e: IOException) {
            emit(Result.Error("Network Error: ${e.message}"))
        } catch (e: HttpException) {
            emit(Result.Error("HTTP Error: ${e.message}"))
        } catch (e: Exception) {
            emit(Result.Error("Unknown Error: ${e.message}"))
        }
    }
    
    // New method to fetch and store flight data
    suspend fun fetchAndStoreFlightData(flightNumber: String): Result<Flight?> {
        return try {
            val response = apiService.getFlightByNumber(apiKey, flightNumber)
            
            if (response.isSuccessful) {
                val flightResponse = response.body()
                val flight = flightResponse?.data?.firstOrNull()
                
                if (flight != null) {
                    // Convert Flight to FlightRecord and store in database
                    val flightRecord = convertToFlightRecord(flight)
                    if (flightRecord != null) {
                        flightRecordDao.insertFlightRecord(flightRecord)
                        
                        // Update route statistics
                        updateRouteStatistics(flightRecord)
                    }
                }
                
                Result.Success(flight)
            } else {
                Result.Error("API Error: ${response.message()}")
            }
        } catch (e: IOException) {
            Result.Error("Network Error: ${e.message}")
        } catch (e: HttpException) {
            Result.Error("HTTP Error: ${e.message}")
        } catch (e: Exception) {
            Result.Error("Unknown Error: ${e.message}")
        }
    }
    
    // Save a flight directly to the database
    suspend fun saveFlight(flight: Flight, isInitialTracking: Boolean = false) {
        try {
            val flightNumber = flight.flightInfo?.iata ?: return
            
            // Convert the flight to a record
            val flightRecord = convertToFlightRecord(flight)
            if (flightRecord != null) {
                if (isInitialTracking) {
                    // For initial tracking, always insert a new record
                    flightRecordDao.insertFlightRecord(flightRecord)
                    // Update route statistics
                    updateRouteStatistics(flightRecord)
                } else {
                    // For minute-by-minute tracking, update the existing record
                    val existingRecord = flightRecordDao.getLatestFlightRecord(flightNumber)
                    if (existingRecord != null) {
                        // Update the existing record
                        val updatedRecord = flightRecord.copy(
                            id = existingRecord.id,
                            recordDate = System.currentTimeMillis() // Update the record date to now
                        )
                        flightRecordDao.insertFlightRecord(updatedRecord)
                        // Update route statistics
                        updateRouteStatistics(updatedRecord)
                    } else {
                        // If no record exists (shouldn't happen), create a new one
                        flightRecordDao.insertFlightRecord(flightRecord)
                        // Update route statistics
                        updateRouteStatistics(flightRecord)
                    }
                }
            }
        } catch (e: Exception) {
            // Handle any exceptions during saving
        }
    }
    
    private fun convertToFlightRecord(flight: Flight): FlightRecord? {
        try {
            // Extract flight data
            val flightNumber = flight.flightInfo?.iata ?: return null
            val airline = flight.airline?.name ?: "Unknown Airline"
            
            val departureAirport = flight.departure?.iata ?: return null
            val departureCity = flight.departure?.airport ?: "Unknown City"
            
            val arrivalAirport = flight.arrival?.iata ?: return null
            val arrivalCity = flight.arrival?.airport ?: "Unknown City"
            
            // Parse departure and arrival times
            val scheduledDepartureTime = parseDateTime(flight.departure?.scheduled)
            val actualDepartureTime = parseDateTime(flight.departure?.actual)
            val scheduledArrivalTime = parseDateTime(flight.arrival?.scheduled)
            val actualArrivalTime = parseDateTime(flight.arrival?.actual)
            
            // Get delay information
            val departureDelayMinutes = flight.departure?.delay
            val arrivalDelayMinutes = flight.arrival?.delay
            
            // Current flight date
            val flightDate = flight.flightDate ?: getTodayDate()
            
            // Calculate the flight time in minutes
            val flightTimeMinutes = if (actualArrivalTime != null && actualDepartureTime != null) {
                // Use actual times if available
                ((actualArrivalTime - actualDepartureTime) / 60_000).toInt()
            } else if (scheduledDepartureTime != null && scheduledArrivalTime != null) {
                // Fall back to scheduled times plus delay
                val departureDelayMs = departureDelayMinutes?.times(60_000L) ?: 0L
                val arrivalDelayMs = arrivalDelayMinutes?.times(60_000L) ?: 0L
                
                (((scheduledArrivalTime + arrivalDelayMs) - (scheduledDepartureTime + departureDelayMs)) / 60_000).toInt()
            } else {
                null
            }
            
            // Create and return FlightRecord
            return FlightRecord(
                flightNumber = flightNumber,
                airline = airline,
                departureAirport = departureAirport,
                departureCity = departureCity,
                arrivalAirport = arrivalAirport,
                arrivalCity = arrivalCity,
                scheduledDepartureTime = scheduledDepartureTime ?: 0L,
                actualDepartureTime = actualDepartureTime,
                scheduledArrivalTime = scheduledArrivalTime ?: 0L,
                actualArrivalTime = actualArrivalTime,
                departureDelayMinutes = departureDelayMinutes,
                arrivalDelayMinutes = arrivalDelayMinutes,
                flightTime = flightTimeMinutes,
                flightDate = flightDate
            )
        } catch (e: Exception) {
            return null
        }
    }
    
    // Get all flight records
    fun getAllFlightRecords(): Flow<List<FlightRecord>> {
        return flightRecordDao.getAllFlightRecords()
    }
    
    // Get all route statistics
    fun getAllRouteStatistics(): Flow<List<RouteStatisticEntity>> {
        return routeStatisticDao.getAllRouteStatistics()
    }
    
    // Get average flight time for a route (keep for backward compatibility)
    suspend fun getAverageFlightTimeForRoute(
        departureAirport: String,
        arrivalAirport: String,
        daysToLookBack: Int = 7
    ): Long? {
        val startDate = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -daysToLookBack)
        }.timeInMillis
        
        // Get from flight records in minutes and convert to milliseconds for compatibility with older code
        val avgMinutes = flightRecordDao.getAverageFlightTimeForRoute(
            departureAirport,
            arrivalAirport,
            startDate
        )
        
        return avgMinutes?.toLong()?.times(60_000L)
    }
    
    // Get all unique routes in the database
    suspend fun getAllUniqueRoutes(): List<RouteInfo> {
        return flightRecordDao.getAllUniqueRoutes()
    }
    
    // Get the number of flights for a specific route
    suspend fun getFlightCountForRoute(
        departureAirport: String,
        arrivalAirport: String,
        daysToLookBack: Int = 7
    ): Int {
        val startDate = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -daysToLookBack)
        }.timeInMillis
        
        return flightRecordDao.getFlightCountForRoute(
            departureAirport,
            arrivalAirport,
            startDate
        )
    }
    
    // Format average time in a user-friendly way
    fun formatAverageTime(minutes: Int): String {
        val hours = minutes / 60
        val mins = minutes % 60
        return "${hours}h ${mins}m"
    }
    
    // Private method to update route statistics when a new flight record is added
    private suspend fun updateRouteStatistics(flightRecord: FlightRecord) {
        try {
            val departureAirport = flightRecord.departureAirport
            val arrivalAirport = flightRecord.arrivalAirport
            
            // Get count of distinct flights for this route
            val flightCount = flightRecordDao.getDistinctFlightCountForRoute(
                departureAirport,
                arrivalAirport
            )
            
            // Calculate flight time for this record in minutes
            val flightTimeMinutes = calculateFlightTimeMinutes(flightRecord)
            
            // Get existing route statistic if it exists
            val existingStatistic = routeStatisticDao.getRouteStatistic(departureAirport, arrivalAirport)
            
            // Calculate average flight time for this route
            val avgTime = flightRecordDao.getAverageFlightTimeForRoute(
                departureAirport,
                arrivalAirport,
                0L // Start from beginning of time to include all records
            ) ?: flightTimeMinutes // Default to current flight time if no average available
            
            if (existingStatistic == null) {
                // Create new route statistic
                val newStatistic = RouteStatisticEntity(
                    departureAirport = departureAirport,
                    departureCity = flightRecord.departureCity,
                    arrivalAirport = arrivalAirport,
                    arrivalCity = flightRecord.arrivalCity,
                    averageFlightTimeMinutes = avgTime,
                    flightCount = flightCount
                )
                routeStatisticDao.insertOrUpdateRouteStatistic(newStatistic)
            } else {
                // Update existing route statistic
                routeStatisticDao.updateRouteStatistics(
                    departureAirport = departureAirport,
                    arrivalAirport = arrivalAirport,
                    flightCount = flightCount,
                    averageTimeMinutes = avgTime
                )
            }
        } catch (e: Exception) {
            // Handle exceptions
        }
    }
    
    // Calculate the flight time for a flight record in minutes
    private fun calculateFlightTimeMinutes(flightRecord: FlightRecord): Int {
        // If the flightTime field is populated, use it directly
        if (flightRecord.flightTime != null) {
            return flightRecord.flightTime
        }
        
        // Otherwise calculate it as before
        val flightTimeMillis = if (flightRecord.actualArrivalTime != null && flightRecord.actualDepartureTime != null) {
            // Use actual times if available
            flightRecord.actualArrivalTime - flightRecord.actualDepartureTime
        } else {
            // Fall back to scheduled times plus delay
            val departureDelay = flightRecord.departureDelayMinutes?.times(60_000L) ?: 0L
            val arrivalDelay = flightRecord.departureDelayMinutes?.times(60_000L) ?: 0L
            
            val scheduledDuration = flightRecord.scheduledArrivalTime - flightRecord.scheduledDepartureTime
            scheduledDuration + arrivalDelay - departureDelay
        }
        
        // Convert milliseconds to minutes
        return (flightTimeMillis / 60_000).toInt()
    }
    
    // Clean up old records and statistics
    suspend fun cleanUpOldRecords(daysToKeep: Int = 30) {
        val cutOffDate = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -daysToKeep)
        }.timeInMillis
        
        flightRecordDao.deleteOldRecords(cutOffDate)
        routeStatisticDao.deleteOldRouteStatistics(cutOffDate)
    }
    
    // Helper methods for date handling
    private fun parseDateTime(isoDateTime: String?): Long? {
        if (isoDateTime == null) return null
        
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val date = inputFormat.parse(isoDateTime) ?: return null
            date.time
        } catch (e: Exception) {
            null
        }
    }
    
    private fun getTodayDate(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateFormat.format(Date())
    }
    
    // New method to get last flight record from the database
    suspend fun getLastFlightRecord(): FlightRecord? {
        // Collects the first flight record from the Flow (ordered by recordDate DESC)
        return flightRecordDao.getAllFlightRecords().first().firstOrNull()
    }

    // New method to fetch and store flight data by route
    suspend fun fetchAndStoreFlightDataByRoute(departureAirport: String, arrivalAirport: String): Result<Flight?> {
        return try {
            val response = apiService.getFlightByRoute(apiKey, departureAirport, arrivalAirport)
            if (response.isSuccessful) {
                val flightResponse = response.body()
                val flight = flightResponse?.data?.firstOrNull()
                if (flight != null) {
                    val flightRecord = convertToFlightRecord(flight)
                    if (flightRecord != null) {
                        flightRecordDao.insertFlightRecord(flightRecord)
                        
                        // Update route statistics
                        updateRouteStatistics(flightRecord)
                    }
                }
                Result.Success(flight)
            } else {
                Result.Error("API Error: ${response.message()}")
            }
        } catch (e: IOException) {
            Result.Error("Network Error: ${e.message}")
        } catch (e: HttpException) {
            Result.Error("HTTP Error: ${e.message}")
        } catch (e: Exception) {
            Result.Error("Unknown Error: ${e.message}")
        }
    }
}

sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String) : Result<Nothing>()
    object Loading : Result<Nothing>()
}