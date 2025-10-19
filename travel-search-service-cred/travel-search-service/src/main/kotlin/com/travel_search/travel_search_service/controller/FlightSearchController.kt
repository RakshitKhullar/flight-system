package com.travel_search.travel_search_service.controller

import com.travel_search.travel_search_service.dto.FlightSearchRequest
import com.travel_search.travel_search_service.dto.FlightSearchResponse
import com.travel_search.travel_search_service.dto.SortBy
import com.travel_search.travel_search_service.service.FlightSearchService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1")
class FlightSearchController(
    private val flightSearchService: FlightSearchService
) {

    @GetMapping("/search/flights")
    fun searchFlights(
        @RequestParam source: String,
        @RequestParam destination: String,
        @RequestParam flightDate: String,
        @RequestParam(required = false, defaultValue = "") departner: List<String>,
        @RequestParam(required = false, defaultValue = "0") maximumStops: List<Int>,
        @RequestParam(required = false, defaultValue = "price") sortBy: String,
    ): ResponseEntity<FlightSearchResponse> {
        
        return try {
            // Parse date string to LocalDate
            val parsedFlightDate = parseDate(flightDate)
            
            // Parse sortBy string to enum
            val parsedSortBy = parseSortBy(sortBy)
            
            val request = FlightSearchRequest(
                src = source,
                destination = destination,
                startDate = parsedFlightDate,
                departner = departner.filter { it.isNotBlank() },
                maximumStops = maximumStops,
                sortBy = parsedSortBy
            )
            
            val response = flightSearchService.searchFlights(request)
            ResponseEntity.ok(response)
            
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).build()
        }
    }
    
    private fun parseDate(dateString: String): java.time.LocalDate {
        // Simple date parsing - in production, use proper date parsing with format validation
        return try {
            java.time.LocalDate.parse(dateString)
        } catch (e: Exception) {
            // Default to tomorrow if parsing fails
            java.time.LocalDate.now().plusDays(1)
        }
    }
    
    private fun parseSortBy(sortByString: String): SortBy {
        return try {
            when (sortByString.lowercase()) {
                "price" -> SortBy.PRICE
                "time" -> SortBy.TIME
                "duration" -> SortBy.DURATION
                "stops" -> SortBy.STOPS
                else -> SortBy.PRICE // Default to PRICE
            }
        } catch (e: Exception) {
            SortBy.PRICE // Default to PRICE if parsing fails
        }
    }
}
