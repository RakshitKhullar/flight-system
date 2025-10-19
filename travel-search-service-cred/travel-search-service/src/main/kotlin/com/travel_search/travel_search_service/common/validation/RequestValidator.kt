package com.travel_search.travel_search_service.common.validation

import com.travel_search.travel_search_service.dto.ElasticSearchRequest
import com.travel_search.travel_search_service.dto.FlightSearchRequest
import java.time.LocalDate

object RequestValidator {
    
    fun validateFlightSearchRequest(request: FlightSearchRequest) {
        require(request.src.isNotBlank()) { "Source cannot be blank" }
        require(request.destination.isNotBlank()) { "Destination cannot be blank" }
        require(request.startDate.isAfter(LocalDate.now().minusDays(1))) { "Flight date must be today or in the future" }
        require(request.maximumStops.all { it >= 0 }) { "Maximum stops cannot be negative" }
    }
    
    fun validateElasticSearchRequest(request: ElasticSearchRequest) {
        require(request.flightId.isNotBlank()) { "Flight ID cannot be blank" }
        require(request.source.isNotBlank()) { "Source cannot be blank" }
        require(request.destination.isNotBlank()) { "Destination cannot be blank" }
        require(request.maximumStops >= 0) { "Maximum stops cannot be negative" }
        require(request.maximumStops <= 2) { "Maximum stops cannot exceed 2" }
    }
}
