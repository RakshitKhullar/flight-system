package com.travel_search.travel_search_service.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.travel_search.travel_search_service.dto.ElasticSearchRequest
import com.travel_search.travel_search_service.repository.ElasticSearchRepository
import com.travel_search.travel_search_service.repository.entity.ElasticSearch
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import java.time.LocalDate
import java.time.ZonedDateTime

@ExtendWith(MockitoExtension::class)
class ElasticSearchServiceTest {

    @Mock
    private lateinit var elasticSearchRepository: ElasticSearchRepository

    private lateinit var elasticSearchService: ElasticSearchService

    private lateinit var sampleElasticSearch: ElasticSearch
    private lateinit var sampleRequest: ElasticSearchRequest

    @BeforeEach
    fun setUp() {
        elasticSearchService = ElasticSearchService(elasticSearchRepository)
        
        val objectMapper = ObjectMapper()
        val seatStructure = objectMapper.readTree("""{"economy": 150, "business": 20, "first": 10}""")
        
        sampleElasticSearch = ElasticSearch(
            id = 1,
            flightId = "AI101",
            source = "Delhi",
            destination = "Mumbai",
            flightDate = LocalDate.now().plusDays(1),
            maximumStops = 0,
            departner = "Air India",
            seatStructure = seatStructure,
            createdAt = ZonedDateTime.now(),
            updatedAt = ZonedDateTime.now()
        )
        
        sampleRequest = ElasticSearchRequest(
            flightId = "AI101",
            source = "Delhi",
            destination = "Mumbai",
            flightDate = LocalDate.now().plusDays(1),
            maximumStops = 0,
            departner = "Air India",
            seatStructure = seatStructure
        )
    }

    @Test
    fun `should add new flight data when no existing flight found`() {
        // Given
        whenever(elasticSearchRepository.findByFlightIdAndCriteria(
            eq("AI101"), 
            eq("Delhi"), 
            eq("Mumbai"), 
            any(), 
            eq(0), 
            eq("Air India")
        )).thenReturn(null)
        whenever(elasticSearchRepository.save(any<ElasticSearch>()))
            .thenReturn(sampleElasticSearch)

        // When
        val result = elasticSearchService.addOrUpdateFlightData(sampleRequest)

        // Then
        assertNotNull(result)
        assertEquals("AI101", result.flightId)
        assertEquals("Delhi", result.source)
        assertEquals("Mumbai", result.destination)
        assertTrue(result.isNewEntry)
        assertEquals("New flight data added successfully", result.message)
    }

    @Test
    fun `should update seat structure when exact match found`() {
        // Given
        val objectMapper = ObjectMapper()
        val newSeatStructure = objectMapper.readTree("""{"economy": 140, "business": 25, "first": 15}""")
        val requestWithNewSeats = sampleRequest.copy(seatStructure = newSeatStructure)
        
        whenever(elasticSearchRepository.findByFlightIdAndCriteria(any(), any(), any(), any(), any(), any()))
            .thenReturn(sampleElasticSearch)
        whenever(elasticSearchRepository.save(any<ElasticSearch>()))
            .thenReturn(sampleElasticSearch.copy(seatStructure = newSeatStructure))

        // When
        val result = elasticSearchService.addOrUpdateFlightData(requestWithNewSeats)

        // Then
        assertNotNull(result)
        assertEquals("AI101", result.flightId)
        assertFalse(result.isNewEntry)
        assertEquals("Seat structure updated successfully", result.message)
    }

    @Test
    fun `should get flights by flight ID`() {
        // Given
        val flightId = "AI101"
        whenever(elasticSearchRepository.findAllByFlightId(flightId))
            .thenReturn(listOf(sampleElasticSearch))

        // When
        val result = elasticSearchService.getFlightsByFlightId(flightId)

        // Then
        assertNotNull(result)
        assertEquals(1, result.size)
        assertEquals("AI101", result[0].flightId)
        assertEquals("Flight data retrieved successfully", result[0].message)
    }

    @Test
    fun `should return empty list when no flights found by flight ID`() {
        // Given
        val flightId = "NONEXISTENT"
        whenever(elasticSearchRepository.findAllByFlightId(flightId))
            .thenReturn(emptyList())

        // When
        val result = elasticSearchService.getFlightsByFlightId(flightId)

        // Then
        assertNotNull(result)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `should throw exception when flight ID is blank`() {
        // Given
        val invalidRequest = sampleRequest.copy(flightId = "")

        // When & Then
        assertThrows(IllegalArgumentException::class.java) {
            elasticSearchService.addOrUpdateFlightData(invalidRequest)
        }
    }

    @Test
    fun `should throw exception when source is blank`() {
        // Given
        val invalidRequest = sampleRequest.copy(source = "")

        // When & Then
        assertThrows(IllegalArgumentException::class.java) {
            elasticSearchService.addOrUpdateFlightData(invalidRequest)
        }
    }

    @Test
    fun `should throw exception when destination is blank`() {
        // Given
        val invalidRequest = sampleRequest.copy(destination = "")

        // When & Then
        assertThrows(IllegalArgumentException::class.java) {
            elasticSearchService.addOrUpdateFlightData(invalidRequest)
        }
    }

    @Test
    fun `should throw exception when maximum stops is negative`() {
        // Given
        val invalidRequest = sampleRequest.copy(maximumStops = -1)

        // When & Then
        assertThrows(IllegalArgumentException::class.java) {
            elasticSearchService.addOrUpdateFlightData(invalidRequest)
        }
    }

    @Test
    fun `should throw exception when maximum stops exceeds 2`() {
        // Given
        val invalidRequest = sampleRequest.copy(maximumStops = 3)

        // When & Then
        assertThrows(IllegalArgumentException::class.java) {
            elasticSearchService.addOrUpdateFlightData(invalidRequest)
        }
    }

    @Test
    fun `should handle null departner correctly`() {
        // Given
        val requestWithNullDepartner = sampleRequest.copy(departner = null)
        whenever(elasticSearchRepository.findByFlightIdAndCriteria(
            eq("AI101"), 
            eq("Delhi"), 
            eq("Mumbai"), 
            any(), 
            eq(0), 
            eq(null)
        )).thenReturn(null)
        whenever(elasticSearchRepository.save(any<ElasticSearch>()))
            .thenReturn(sampleElasticSearch.copy(departner = null))

        // When
        val result = elasticSearchService.addOrUpdateFlightData(requestWithNullDepartner)

        // Then
        assertNotNull(result)
        assertEquals("AI101", result.flightId)
        assertTrue(result.isNewEntry)
    }
}
