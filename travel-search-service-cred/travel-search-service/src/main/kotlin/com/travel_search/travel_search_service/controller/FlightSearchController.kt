package com.travel_search.travel_search_service.controller

import com.travel_search.travel_search_service.dto.FlightSearchRequest
import com.travel_search.travel_search_service.dto.FlightSearchResponse
import com.travel_search.travel_search_service.dto.SortBy
import com.travel_search.travel_search_service.service.interfaces.IFlightSearchService
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1")
class FlightSearchController(
    private val flightSearchService: IFlightSearchService
) {

    @GetMapping("/search/flights")
    fun searchFlights(
        @RequestParam source: String,
        @RequestParam destination: String,
        @RequestParam flightDate: String,
        @RequestParam(required = false, defaultValue = "") departner: List<String>,
        @RequestParam(required = false, defaultValue = "0") maximumStops: List<Int>,
        @RequestParam(required = false, defaultValue = "price") sortBy: String,
    ): FlightSearchResponse {
        val request = FlightSearchRequest(
            src = source,
            destination = destination,
            startDate = parseDate(flightDate),
            departner = departner.filter { it.isNotBlank() },
            maximumStops = maximumStops,
            sortBy = parseSortBy(sortBy)
        )
        
        return flightSearchService.searchFlights(request)
    }
    
    private fun parseDate(dateString: String): java.time.LocalDate =
        try {
            java.time.LocalDate.parse(dateString)
        } catch (e: Exception) {
            java.time.LocalDate.now().plusDays(1)
        }
    
    private fun parseSortBy(sortByString: String): SortBy =
        when (sortByString.lowercase()) {
            "price" -> SortBy.PRICE
            "time" -> SortBy.TIME
            "duration" -> SortBy.DURATION
            "stops" -> SortBy.STOPS
            else -> SortBy.PRICE
        }
}
