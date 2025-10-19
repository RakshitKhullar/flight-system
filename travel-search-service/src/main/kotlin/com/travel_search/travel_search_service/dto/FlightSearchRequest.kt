package com.travel_search.travel_search_service.dto

import java.time.LocalDate

enum class SortBy {
    PRICE,
    TIME,
    DURATION,
    STOPS
}

data class FlightSearchRequest(
    val src: String,
    val destination: String,
    val startDate: LocalDate,
    val departner: List<String>,
    val maximumStops: List<Int>,
    val sortBy: SortBy = SortBy.PRICE
)
