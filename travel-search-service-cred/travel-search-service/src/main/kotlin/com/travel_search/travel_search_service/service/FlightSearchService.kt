package com.travel_search.travel_search_service.service

import com.travel_search.travel_search_service.common.mapper.EntityMapper
import com.travel_search.travel_search_service.common.utils.CacheKeyGenerator
import com.travel_search.travel_search_service.common.utils.FlightUtils
import com.travel_search.travel_search_service.common.validation.RequestValidator
import com.travel_search.travel_search_service.dto.FlightSearchRequest
import com.travel_search.travel_search_service.dto.FlightSearchResponse
import com.travel_search.travel_search_service.repository.ElasticSearchRepository
import com.travel_search.travel_search_service.repository.entity.ElasticSearch
import com.travel_search.travel_search_service.service.interfaces.IFlightSearchService
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service

@Service
class FlightSearchService(
    private val elasticSearchRepository: ElasticSearchRepository,
    private val redisTemplate: RedisTemplate<String, Any>
) : IFlightSearchService {

    @Cacheable(value = ["flightSearch"], key = "#request.src + '-' + #request.destination + '-' + #request.startDate + '-' + #request.sortBy")
    override fun searchFlights(request: FlightSearchRequest): FlightSearchResponse {
        RequestValidator.validateFlightSearchRequest(request)
        
        return searchFlightsFromDatabase(request)
    }
    
    private fun searchFlightsFromDatabase(request: FlightSearchRequest): FlightSearchResponse {
        val dbFlights = fetchFlightsFromDatabase(request)
        val flights = dbFlights.map { EntityMapper.toFlightDto(it) }
        val sortedFlights = FlightUtils.sortFlights(flights, request.sortBy)
        
        return FlightSearchResponse(
            flights = sortedFlights,
            totalResults = sortedFlights.size,
            currentPage = 1,
            totalPages = 1
        )
    }
    
    private fun fetchFlightsFromDatabase(request: FlightSearchRequest) =
        elasticSearchRepository.findFlightsBySearchCriteria(
            source = request.src,
            destination = request.destination,
            flightDate = request.startDate,
            departner = request.departner.firstOrNull(),
            maxStops = request.maximumStops.maxOrNull() ?: 2
        )
}
