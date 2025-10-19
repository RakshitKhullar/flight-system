package com.travel_search.travel_search_service.dto

import com.fasterxml.jackson.databind.JsonNode
import java.time.LocalDate

data class ElasticSearchRequest(
    val flightId: String,
    val source: String,
    val destination: String,
    val flightDate: LocalDate,
    val maximumStops: Int,
    val departner: String?,
    val seatStructure: JsonNode?
)

data class ElasticSearchResponse(
    val id: Long,
    val flightId: String,
    val source: String,
    val destination: String,
    val flightDate: LocalDate,
    val maximumStops: Int,
    val departner: String?,
    val seatStructure: JsonNode?,
    val message: String,
    val isNewEntry: Boolean
)
