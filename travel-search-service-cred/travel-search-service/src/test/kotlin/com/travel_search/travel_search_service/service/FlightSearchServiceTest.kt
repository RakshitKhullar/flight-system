package com.travel_search.travel_search_service.service

import com.travel_search.travel_search_service.dto.FlightSearchRequest
import com.travel_search.travel_search_service.dto.SortBy
import com.travel_search.travel_search_service.repository.ElasticSearchRepository
import com.travel_search.travel_search_service.repository.entity.ElasticSearch
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.data.redis.core.RedisTemplate
import java.time.LocalDate
import java.time.ZonedDateTime

@ExtendWith(MockitoExtension::class)
class FlightSearchServiceTest {

    @Mock
    private lateinit var elasticSearchRepository: ElasticSearchRepository

    @Mock
    private lateinit var redisTemplate: RedisTemplate<String, Any>

    private lateinit var flightSearchService: FlightSearchService

    private lateinit var sampleFlights: List<ElasticSearch>

    @BeforeEach
    fun setUp() {
        flightSearchService = FlightSearchService(elasticSearchRepository, redisTemplate)
        
        sampleFlights = listOf(
            ElasticSearch(
                id = 1,
                flightId = "AI101",
                source = "Delhi",
                destination = "Mumbai",
                flightDate = LocalDate.now().plusDays(1),
                maximumStops = 0,
                departner = "Air India",
                seatStructure = null,
                createdAt = ZonedDateTime.now(),
                updatedAt = ZonedDateTime.now()
            ),
            ElasticSearch(
                id = 2,
                flightId = "6E202",
                source = "Delhi",
                destination = "Mumbai",
                flightDate = LocalDate.now().plusDays(1),
                maximumStops = 1,
                departner = "IndiGo",
                seatStructure = null,
                createdAt = ZonedDateTime.now(),
                updatedAt = ZonedDateTime.now()
            )
        )
    }

    @Test
    fun `should return flight search results when valid request provided`() {
        // Given
        val request = FlightSearchRequest(
            src = "Delhi",
            destination = "Mumbai",
            startDate = LocalDate.now().plusDays(1),
            departner = listOf("Air India"),
            maximumStops = listOf(0, 1),
            sortBy = SortBy.PRICE
        )

        whenever(elasticSearchRepository.findFlightsBySearchCriteria(any(), any(), any(), any(), any()))
            .thenReturn(sampleFlights)

        // When
        val result = flightSearchService.searchFlights(request)

        // Then
        assertNotNull(result)
        assertEquals(2, result.totalResults)
        assertEquals(2, result.flights.size)
        assertEquals(1, result.currentPage)
        assertEquals(1, result.totalPages)
        
        // Verify flight details
        val firstFlight = result.flights[0]
        assertEquals("AI101", firstFlight.flightId)
        assertEquals("Delhi", firstFlight.src)
        assertEquals("Mumbai", firstFlight.destination)
    }

    @Test
    fun `should return empty results when no flights found`() {
        // Given
        val request = FlightSearchRequest(
            src = "Delhi",
            destination = "Chennai",
            startDate = LocalDate.now().plusDays(1),
            departner = emptyList(),
            maximumStops = listOf(0),
            sortBy = SortBy.PRICE
        )

        whenever(elasticSearchRepository.findFlightsBySearchCriteria(any(), any(), any(), any(), any()))
            .thenReturn(emptyList())

        // When
        val result = flightSearchService.searchFlights(request)

        // Then
        assertNotNull(result)
        assertEquals(0, result.totalResults)
        assertTrue(result.flights.isEmpty())
    }

    @Test
    fun `should throw exception when source is blank`() {
        // Given
        val request = FlightSearchRequest(
            src = "",
            destination = "Mumbai",
            startDate = LocalDate.now().plusDays(1),
            departner = emptyList(),
            maximumStops = listOf(0),
            sortBy = SortBy.PRICE
        )

        // When & Then
        assertThrows(IllegalArgumentException::class.java) {
            flightSearchService.searchFlights(request)
        }
    }

    @Test
    fun `should throw exception when destination is blank`() {
        // Given
        val request = FlightSearchRequest(
            src = "Delhi",
            destination = "",
            startDate = LocalDate.now().plusDays(1),
            departner = emptyList(),
            maximumStops = listOf(0),
            sortBy = SortBy.PRICE
        )

        // When & Then
        assertThrows(IllegalArgumentException::class.java) {
            flightSearchService.searchFlights(request)
        }
    }

    @Test
    fun `should throw exception when flight date is in past`() {
        // Given
        val request = FlightSearchRequest(
            src = "Delhi",
            destination = "Mumbai",
            startDate = LocalDate.now().minusDays(1),
            departner = emptyList(),
            maximumStops = listOf(0),
            sortBy = SortBy.PRICE
        )

        // When & Then
        assertThrows(IllegalArgumentException::class.java) {
            flightSearchService.searchFlights(request)
        }
    }

    @Test
    fun `should sort flights by price when sortBy is PRICE`() {
        // Given
        val request = FlightSearchRequest(
            src = "Delhi",
            destination = "Mumbai",
            startDate = LocalDate.now().plusDays(1),
            departner = emptyList(),
            maximumStops = listOf(0, 1),
            sortBy = SortBy.PRICE
        )

        whenever(elasticSearchRepository.findFlightsBySearchCriteria(any(), any(), any(), any(), any()))
            .thenReturn(sampleFlights)

        // When
        val result = flightSearchService.searchFlights(request)

        // Then
        assertNotNull(result)
        assertEquals(2, result.flights.size)
        // Verify flights are sorted by price (first flight should have lower or equal price than second)
        assertTrue(result.flights[0].price <= result.flights[1].price)
    }

    @Test
    fun `should sort flights by stops when sortBy is STOPS`() {
        // Given
        val request = FlightSearchRequest(
            src = "Delhi",
            destination = "Mumbai",
            startDate = LocalDate.now().plusDays(1),
            departner = emptyList(),
            maximumStops = listOf(0, 1),
            sortBy = SortBy.STOPS
        )

        whenever(elasticSearchRepository.findFlightsBySearchCriteria(any(), any(), any(), any(), any()))
            .thenReturn(sampleFlights)

        // When
        val result = flightSearchService.searchFlights(request)

        // Then
        assertNotNull(result)
        assertEquals(2, result.flights.size)
        // Verify flights are sorted by stops
        assertTrue(result.flights[0].stops <= result.flights[1].stops)
    }
}
