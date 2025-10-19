package com.travel_search.travel_search_service.dto

import java.time.LocalDateTime

data class FlightSearchResponse(
    val flights: List<Flight>,
    val totalResults: Int,
    val currentPage: Int,
    val totalPages: Int
)

data class Flight(
    val flightId: String,
    val airline: String,
    val flightNumber: String,
    val src: String,
    val destination: String,
    val departureTime: LocalDateTime,
    val arrivalTime: LocalDateTime,
    val duration: String,
    val stops: Int,
    val price: Double,
    val currency: String,
    val availableSeats: Int,
    val aircraftType: String,
    val returnFlight: Flight? = null
)
