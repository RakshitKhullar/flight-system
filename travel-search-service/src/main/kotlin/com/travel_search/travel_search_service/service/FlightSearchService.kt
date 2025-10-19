package com.travel_search.travel_search_service.service

import com.travel_search.travel_search_service.dto.Flight
import com.travel_search.travel_search_service.dto.FlightSearchRequest
import com.travel_search.travel_search_service.dto.FlightSearchResponse
import com.travel_search.travel_search_service.dto.SortBy
import org.springframework.stereotype.Service

@Service
class FlightSearchService {

    fun searchFlights(request: FlightSearchRequest): FlightSearchResponse {
        // Mock flight data for demonstration
        val mockFlights = generateMockFlights(request)
        
        // Apply filters based on request parameters
        val filteredFlights = mockFlights.filter { flight ->
            // Filter by source and destination
            flight.src.equals(request.src, ignoreCase = true) &&
            flight.destination.equals(request.destination, ignoreCase = true) &&
            
            // Filter by departure date
            flight.departureTime.toLocalDate().isEqual(request.startDate) &&
            
            // Filter by maximum stops
            request.maximumStops.any { maxStops -> flight.stops <= maxStops } &&
            
            // Filter by airline if specified
            request.departner.isEmpty() || request.departner.any { airline ->
                flight.airline.equals(airline, ignoreCase = true)
            }
        }
        
        // Apply sorting based on sortBy parameter
        val sortedFlights = when (request.sortBy) {
            SortBy.PRICE -> filteredFlights.sortedBy { it.price }
            SortBy.TIME -> filteredFlights.sortedBy { it.departureTime }
            SortBy.DURATION -> filteredFlights.sortedBy { parseDuration(it.duration) }
            SortBy.STOPS -> filteredFlights.sortedBy { it.stops }
        }
        
        return FlightSearchResponse(
            flights = sortedFlights,
            totalResults = sortedFlights.size,
            currentPage = 1,
            totalPages = 1
        )
    }
    
    private fun generateMockFlights(request: FlightSearchRequest): List<Flight> {
        // Generate mock flight data
        val airlines = listOf("Air India", "IndiGo", "SpiceJet", "Vistara", "GoAir")
        val mockFlights = mutableListOf<Flight>()
        
        repeat(20) { index ->
            val airline = airlines[index % airlines.size]
            val flightNumber = "${airline.take(2).uppercase()}${(1000..9999).random()}"
            val departureTime = request.startDate.atTime(6 + (index % 18), (index * 15) % 60)
            val duration = "${(1..8).random()}h ${(0..59).random()}m"
            val stops = (0..2).random()
            val price = (5000..25000).random().toDouble()
            
            val flight = Flight(
                flightId = "FL${index + 1}",
                airline = airline,
                flightNumber = flightNumber,
                src = request.src,
                destination = request.destination,
                departureTime = departureTime.plusHours(index.toLong()),
                arrivalTime = departureTime.plusHours(index.toLong() + 2),
                duration = duration,
                stops = stops,
                price = price,
                currency = "INR",
                availableSeats = (10..180).random(),
                aircraftType = "Boeing 737",
                returnFlight = null
            )
            
            mockFlights.add(flight)
        }
        
        return mockFlights
    }
    
    private fun parseDuration(duration: String): Int {
        // Parse duration string like "2h 30m" to minutes for sorting
        return try {
            val parts = duration.split(" ")
            var totalMinutes = 0
            
            for (part in parts) {
                when {
                    part.endsWith("h") -> {
                        val hours = part.dropLast(1).toIntOrNull() ?: 0
                        totalMinutes += hours * 60
                    }
                    part.endsWith("m") -> {
                        val minutes = part.dropLast(1).toIntOrNull() ?: 0
                        totalMinutes += minutes
                    }
                }
            }
            totalMinutes
        } catch (e: Exception) {
            0 // Default to 0 if parsing fails
        }
    }
}
