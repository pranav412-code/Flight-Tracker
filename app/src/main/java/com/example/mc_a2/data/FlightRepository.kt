package com.example.mc_a2.data

import com.example.mc_a2.data.api.FlightApiService
import com.example.mc_a2.data.api.NetworkModule
import com.example.mc_a2.data.db.FlightDatabase
import com.example.mc_a2.data.db.FlightRecord
import com.example.mc_a2.data.db.RouteInfo
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

class FlightRepository(private val database: FlightDatabase) {
    private val flightRecordDao = database.flightRecordDao()
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
    suspend fun saveFlight(flight: Flight) {
        try {
            val flightRecord = convertToFlightRecord(flight)
            if (flightRecord != null) {
                flightRecordDao.insertFlightRecord(flightRecord)
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
    
    // Get average flight time for a route
    suspend fun getAverageFlightTimeForRoute(
        departureAirport: String,
        arrivalAirport: String,
        daysToLookBack: Int = 7
    ): Long? {
        val startDate = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -daysToLookBack)
        }.timeInMillis
        
        return flightRecordDao.getAverageFlightTimeForRoute(
            departureAirport,
            arrivalAirport,
            startDate
        )
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
    fun formatAverageTime(milliseconds: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(milliseconds)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds) % 60
        return "${hours}h ${minutes}m"
    }
    
    // Clean up old records
    suspend fun cleanUpOldRecords(daysToKeep: Int = 30) {
        val cutOffDate = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -daysToKeep)
        }.timeInMillis
        
        flightRecordDao.deleteOldRecords(cutOffDate)
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
            // Assumes apiService has a method getFlightByRoute; adjust endpoint and parameters as needed.
            val response = apiService.getFlightByRoute(apiKey, departureAirport, arrivalAirport)
            if (response.isSuccessful) {
                val flightResponse = response.body()
                val flight = flightResponse?.data?.firstOrNull()
                if (flight != null) {
                    val flightRecord = convertToFlightRecord(flight)
                    if (flightRecord != null) {
                        flightRecordDao.insertFlightRecord(flightRecord)
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