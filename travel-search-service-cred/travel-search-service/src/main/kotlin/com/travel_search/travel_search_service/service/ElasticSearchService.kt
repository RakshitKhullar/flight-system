package com.travel_search.travel_search_service.service

import com.travel_search.travel_search_service.common.constants.Messages
import com.travel_search.travel_search_service.common.mapper.EntityMapper
import com.travel_search.travel_search_service.common.validation.RequestValidator
import com.travel_search.travel_search_service.dto.ElasticSearchRequest
import com.travel_search.travel_search_service.dto.ElasticSearchResponse
import com.travel_search.travel_search_service.repository.ElasticSearchRepository
import com.travel_search.travel_search_service.repository.entity.ElasticSearch
import com.travel_search.travel_search_service.service.interfaces.IElasticSearchService
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime

@Service
@Transactional
class ElasticSearchService(
    private val elasticSearchRepository: ElasticSearchRepository
) : IElasticSearchService {

    @CacheEvict(value = ["flightSearch"], allEntries = true)
    override fun addOrUpdateFlightData(request: ElasticSearchRequest): ElasticSearchResponse {
        RequestValidator.validateElasticSearchRequest(request)
        
        val exactMatch = findExactMatch(request)
        
        return if (exactMatch != null) {
            updateSeatStructure(exactMatch, request)
        } else {
            addNewFlightData(request)
        }
    }
    
    private fun findExactMatch(request: ElasticSearchRequest) =
        elasticSearchRepository.findByFlightIdAndCriteria(
            flightId = request.flightId,
            source = request.source,
            destination = request.destination,
            flightDate = request.flightDate,
            maximumStops = request.maximumStops,
            departner = request.departner
        )
    
    private fun addNewFlightData(request: ElasticSearchRequest): ElasticSearchResponse {
        val newFlight = EntityMapper.toEntity(request)
        val savedFlight = elasticSearchRepository.save(newFlight)
        return EntityMapper.toResponse(savedFlight, Messages.FLIGHT_DATA_ADDED, true)
    }
    
    private fun updateSeatStructure(existingFlight: ElasticSearch, request: ElasticSearchRequest): ElasticSearchResponse {
        val updatedFlight = existingFlight.copy(
            seatStructure = request.seatStructure,
            updatedAt = ZonedDateTime.now()
        )
        val savedFlight = elasticSearchRepository.save(updatedFlight)
        return EntityMapper.toResponse(savedFlight, Messages.SEAT_STRUCTURE_UPDATED, false)
    }
    
    @Cacheable(value = ["elasticSearch"], key = "#flightId")
    override fun getFlightsByFlightId(flightId: String): List<ElasticSearchResponse> =
        elasticSearchRepository.findAllByFlightId(flightId)
            .map { EntityMapper.toResponse(it, Messages.FLIGHT_DATA_RETRIEVED, false) }
}
