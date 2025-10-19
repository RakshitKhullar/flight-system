package com.travel_search.travel_search_service.service.interfaces

import com.travel_search.travel_search_service.dto.ElasticSearchRequest
import com.travel_search.travel_search_service.dto.ElasticSearchResponse

interface IElasticSearchService {
    fun addOrUpdateFlightData(request: ElasticSearchRequest): ElasticSearchResponse
    fun getFlightsByFlightId(flightId: String): List<ElasticSearchResponse>
}
