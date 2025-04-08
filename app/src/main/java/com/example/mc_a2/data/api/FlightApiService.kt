package com.example.mc_a2.data.api

import com.example.mc_a2.data.model.FlightResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface FlightApiService {
    @GET("flights")
    suspend fun getFlightByNumber(
        @Query("access_key") accessKey: String,
        @Query("flight_iata") flightIata: String
    ): Response<FlightResponse>
    
    // New method to get flight data by route (departure and arrival airports)
    @GET("flights")
    suspend fun getFlightByRoute(
        @Query("access_key") accessKey: String,
        @Query("dep_iata") departureIata: String,
        @Query("arr_iata") arrivalIata: String
    ): Response<FlightResponse>
}