package com.example.mc_a2.data

import com.example.mc_a2.data.api.NetworkModule
import com.example.mc_a2.data.model.Flight
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException
import java.io.IOException

class FlightRepository {
    private val apiService = NetworkModule.flightApiService
    private val apiKey = NetworkModule.getApiKey()
    
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
}

sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String) : Result<Nothing>()
    object Loading : Result<Nothing>()
}