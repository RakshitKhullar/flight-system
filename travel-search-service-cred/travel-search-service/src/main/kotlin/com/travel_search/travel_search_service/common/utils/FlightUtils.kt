package com.travel_search.travel_search_service.common.utils

import com.travel_search.travel_search_service.dto.Flight
import com.travel_search.travel_search_service.dto.SortBy

object FlightUtils {
    
    fun sortFlights(flights: List<Flight>, sortBy: SortBy): List<Flight> {
        return when (sortBy) {
            SortBy.PRICE -> flights.sortedBy { it.price }
            SortBy.TIME -> flights.sortedBy { it.departureTime }
            SortBy.DURATION -> flights.sortedBy { parseDuration(it.duration) }
            SortBy.STOPS -> flights.sortedBy { it.stops }
        }
    }
    
    private fun parseDuration(duration: String): Int {
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
            0
        }
    }
}
