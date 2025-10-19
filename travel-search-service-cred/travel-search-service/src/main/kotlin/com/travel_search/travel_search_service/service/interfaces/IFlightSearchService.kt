package com.travel_search.travel_search_service.service.interfaces

import com.travel_search.travel_search_service.dto.FlightSearchRequest
import com.travel_search.travel_search_service.dto.FlightSearchResponse

interface IFlightSearchService {
    fun searchFlights(request: FlightSearchRequest): FlightSearchResponse
}
