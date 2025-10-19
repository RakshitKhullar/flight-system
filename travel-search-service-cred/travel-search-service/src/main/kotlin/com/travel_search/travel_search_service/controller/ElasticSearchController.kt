package com.travel_search.travel_search_service.controller

import com.travel_search.travel_search_service.dto.ElasticSearchRequest
import com.travel_search.travel_search_service.dto.ElasticSearchResponse
import com.travel_search.travel_search_service.service.interfaces.IElasticSearchService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/elastic-search")
class ElasticSearchController(
    private val elasticSearchService: IElasticSearchService
) {

    @PostMapping("/flight-data")
    fun addOrUpdateFlightData(@RequestBody request: ElasticSearchRequest): ElasticSearchResponse =
        elasticSearchService.addOrUpdateFlightData(request)

    @GetMapping("/flight/{flightId}")
    fun getFlightById(@PathVariable flightId: String): ResponseEntity<List<ElasticSearchResponse>> {
        val flights = elasticSearchService.getFlightsByFlightId(flightId)
        return if (flights.isNotEmpty()) {
            ResponseEntity.ok(flights)
        } else {
            ResponseEntity.notFound().build()
        }
    }
}
