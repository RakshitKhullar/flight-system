package com.travel_search.travel_search_service.common.mapper

import com.travel_search.travel_search_service.dto.ElasticSearchRequest
import com.travel_search.travel_search_service.dto.ElasticSearchResponse
import com.travel_search.travel_search_service.dto.Flight
import com.travel_search.travel_search_service.repository.entity.ElasticSearch
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZonedDateTime

object EntityMapper {
    
    fun toEntity(request: ElasticSearchRequest): ElasticSearch {
        return ElasticSearch(
            flightId = request.flightId,
            source = request.source,
            destination = request.destination,
            flightDate = request.flightDate,
            maximumStops = request.maximumStops,
            departner = request.departner,
            seatStructure = request.seatStructure,
            createdAt = ZonedDateTime.now(),
            updatedAt = ZonedDateTime.now()
        )
    }
    
    fun toResponse(entity: ElasticSearch, message: String, isNewEntry: Boolean): ElasticSearchResponse {
        return ElasticSearchResponse(
            id = entity.id,
            flightId = entity.flightId,
            source = entity.source,
            destination = entity.destination,
            flightDate = entity.flightDate,
            maximumStops = entity.maximumStops,
            departner = entity.departner,
            seatStructure = entity.seatStructure,
            message = message,
            isNewEntry = isNewEntry
        )
    }
    
    fun toFlightDto(entity: ElasticSearch): Flight {
        val departureTime = generateFlightTime(entity.flightDate)
        val arrivalTime = departureTime.plusHours((1..8).random().toLong())
        val duration = calculateDuration(departureTime, arrivalTime)
        val price = calculatePrice(entity.maximumStops)
        
        return Flight(
            flightId = entity.flightId,
            airline = entity.departner ?: "Unknown Airline",
            flightNumber = generateFlightNumber(entity.departner),
            src = entity.source,
            destination = entity.destination,
            departureTime = departureTime,
            arrivalTime = arrivalTime,
            duration = duration,
            stops = entity.maximumStops,
            price = price,
            currency = "INR",
            availableSeats = (10..180).random(),
            aircraftType = "Boeing 737",
            returnFlight = null
        )
    }
    
    private fun generateFlightTime(flightDate: java.time.LocalDate): LocalDateTime {
        return flightDate.atTime(LocalTime.of((6..22).random(), (0..59).random()))
    }
    
    private fun calculateDuration(departure: LocalDateTime, arrival: LocalDateTime): String {
        val duration = java.time.Duration.between(departure, arrival)
        val hours = duration.toHours()
        val minutes = duration.toMinutes() % 60
        return "${hours}h ${minutes}m"
    }
    
    private fun calculatePrice(stops: Int): Double {
        val basePrice = 5000.0
        val stopsMultiplier = when (stops) {
            0 -> 1.5  // Non-stop flights are more expensive
            1 -> 1.2
            else -> 1.0
        }
        return basePrice * stopsMultiplier + (0..5000).random()
    }
    
    private fun generateFlightNumber(airline: String?): String {
        val prefix = if (airline.isNullOrBlank()) "UK" else airline.take(2).uppercase()
        return "$prefix${(1000..9999).random()}"
    }
}
