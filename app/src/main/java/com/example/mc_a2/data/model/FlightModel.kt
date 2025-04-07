package com.example.mc_a2.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class FlightResponse(
    @Json(name = "pagination") val pagination: Pagination? = null,
    @Json(name = "data") val data: List<Flight>? = null
)

@JsonClass(generateAdapter = true)
data class Pagination(
    @Json(name = "limit") val limit: Int,
    @Json(name = "offset") val offset: Int,
    @Json(name = "count") val count: Int,
    @Json(name = "total") val total: Int
)

@JsonClass(generateAdapter = true)
data class Flight(
    @Json(name = "flight_date") val flightDate: String? = null,
    @Json(name = "flight_status") val flightStatus: String? = null,
    @Json(name = "departure") val departure: DepartureArrival? = null,
    @Json(name = "arrival") val arrival: DepartureArrival? = null,
    @Json(name = "airline") val airline: Airline? = null,
    @Json(name = "flight") val flightInfo: FlightInfo? = null,
    @Json(name = "aircraft") val aircraft: Aircraft? = null,
    @Json(name = "live") val live: LiveData? = null
)

@JsonClass(generateAdapter = true)
data class DepartureArrival(
    @Json(name = "airport") val airport: String? = null,
    @Json(name = "timezone") val timezone: String? = null,
    @Json(name = "iata") val iata: String? = null,
    @Json(name = "icao") val icao: String? = null,
    @Json(name = "terminal") val terminal: String? = null,
    @Json(name = "gate") val gate: String? = null,
    @Json(name = "delay") val delay: Int? = null,
    @Json(name = "scheduled") val scheduled: String? = null,
    @Json(name = "estimated") val estimated: String? = null,
    @Json(name = "actual") val actual: String? = null,
    @Json(name = "estimated_runway") val estimatedRunway: String? = null,
    @Json(name = "actual_runway") val actualRunway: String? = null
)

@JsonClass(generateAdapter = true)
data class Airline(
    @Json(name = "name") val name: String? = null,
    @Json(name = "iata") val iata: String? = null,
    @Json(name = "icao") val icao: String? = null
)

@JsonClass(generateAdapter = true)
data class FlightInfo(
    @Json(name = "number") val number: String? = null,
    @Json(name = "iata") val iata: String? = null,
    @Json(name = "icao") val icao: String? = null,
    @Json(name = "codeshared") val codeshared: CodeShared? = null
)

@JsonClass(generateAdapter = true)
data class CodeShared(
    @Json(name = "airline_name") val airlineName: String? = null,
    @Json(name = "airline_iata") val airlineIata: String? = null,
    @Json(name = "airline_icao") val airlineIcao: String? = null,
    @Json(name = "flight_number") val flightNumber: String? = null,
    @Json(name = "flight_iata") val flightIata: String? = null,
    @Json(name = "flight_icao") val flightIcao: String? = null
)

@JsonClass(generateAdapter = true)
data class Aircraft(
    @Json(name = "registration") val registration: String? = null,
    @Json(name = "iata") val iata: String? = null,
    @Json(name = "icao") val icao: String? = null,
    @Json(name = "icao24") val icao24: String? = null
)

@JsonClass(generateAdapter = true)
data class LiveData(
    @Json(name = "updated") val updated: String? = null,
    @Json(name = "latitude") val latitude: Double? = null,
    @Json(name = "longitude") val longitude: Double? = null,
    @Json(name = "altitude") val altitude: Double? = null,
    @Json(name = "direction") val direction: Double? = null,
    @Json(name = "speed_horizontal") val speedHorizontal: Double? = null,
    @Json(name = "speed_vertical") val speedVertical: Double? = null,
    @Json(name = "is_ground") val isGround: Boolean? = null
)