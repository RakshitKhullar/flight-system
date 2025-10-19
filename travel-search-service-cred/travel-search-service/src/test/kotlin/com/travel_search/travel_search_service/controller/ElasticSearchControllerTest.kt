package com.travel_search.travel_search_service.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.travel_search.travel_search_service.dto.ElasticSearchRequest
import com.travel_search.travel_search_service.dto.ElasticSearchResponse
import com.travel_search.travel_search_service.service.interfaces.IElasticSearchService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.context.junit.jupiter.SpringJUnitExtension
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.LocalDate

@ExtendWith(SpringJUnitExtension::class)
@WebMvcTest(ElasticSearchController::class)
class ElasticSearchControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var elasticSearchService: IElasticSearchService

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    private lateinit var sampleRequest: ElasticSearchRequest
    private lateinit var sampleResponse: ElasticSearchResponse

    @BeforeEach
    fun setUp() {
        val seatStructure = objectMapper.readTree("""{"economy": 150, "business": 20, "first": 10}""")
        
        sampleRequest = ElasticSearchRequest(
            flightId = "AI101",
            source = "Delhi",
            destination = "Mumbai",
            flightDate = LocalDate.now().plusDays(1),
            maximumStops = 0,
            departner = "Air India",
            seatStructure = seatStructure
        )

        sampleResponse = ElasticSearchResponse(
            id = 1,
            flightId = "AI101",
            source = "Delhi",
            destination = "Mumbai",
            flightDate = LocalDate.now().plusDays(1),
            maximumStops = 0,
            departner = "Air India",
            seatStructure = seatStructure,
            message = "New flight data added successfully",
            isNewEntry = true
        )
    }

    @Test
    fun `should add new flight data successfully`() {
        // Given
        whenever(elasticSearchService.addOrUpdateFlightData(any())).thenReturn(sampleResponse)

        // When & Then
        mockMvc.perform(
            post("/api/v1/elastic-search/flight-data")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleRequest))
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.flightId").value("AI101"))
            .andExpect(jsonPath("$.source").value("Delhi"))
            .andExpect(jsonPath("$.destination").value("Mumbai"))
            .andExpect(jsonPath("$.isNewEntry").value(true))
            .andExpect(jsonPath("$.message").value("New flight data added successfully"))
    }

    @Test
    fun `should update existing flight data successfully`() {
        // Given
        val updateResponse = sampleResponse.copy(
            message = "Seat structure updated successfully",
            isNewEntry = false
        )
        whenever(elasticSearchService.addOrUpdateFlightData(any())).thenReturn(updateResponse)

        // When & Then
        mockMvc.perform(
            post("/api/v1/elastic-search/flight-data")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleRequest))
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.flightId").value("AI101"))
            .andExpect(jsonPath("$.isNewEntry").value(false))
            .andExpect(jsonPath("$.message").value("Seat structure updated successfully"))
    }

    @Test
    fun `should get flight by ID successfully`() {
        // Given
        val flightId = "AI101"
        whenever(elasticSearchService.getFlightsByFlightId(flightId))
            .thenReturn(listOf(sampleResponse))

        // When & Then
        mockMvc.perform(
            get("/api/v1/elastic-search/flight/{flightId}", flightId)
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$[0].flightId").value("AI101"))
            .andExpect(jsonPath("$[0].source").value("Delhi"))
            .andExpect(jsonPath("$[0].destination").value("Mumbai"))
    }

    @Test
    fun `should return not found when flight ID does not exist`() {
        // Given
        val flightId = "NONEXISTENT"
        whenever(elasticSearchService.getFlightsByFlightId(flightId))
            .thenReturn(emptyList())

        // When & Then
        mockMvc.perform(
            get("/api/v1/elastic-search/flight/{flightId}", flightId)
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `should handle multiple flights with same flight ID`() {
        // Given
        val flightId = "AI101"
        val secondResponse = sampleResponse.copy(
            id = 2,
            destination = "Bangalore",
            message = "Flight data retrieved successfully"
        )
        whenever(elasticSearchService.getFlightsByFlightId(flightId))
            .thenReturn(listOf(sampleResponse, secondResponse))

        // When & Then
        mockMvc.perform(
            get("/api/v1/elastic-search/flight/{flightId}", flightId)
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].flightId").value("AI101"))
            .andExpect(jsonPath("$[1].flightId").value("AI101"))
            .andExpect(jsonPath("$[0].destination").value("Mumbai"))
            .andExpect(jsonPath("$[1].destination").value("Bangalore"))
    }

    @Test
    fun `should handle request with null departner`() {
        // Given
        val requestWithNullDepartner = sampleRequest.copy(departner = null)
        val responseWithNullDepartner = sampleResponse.copy(departner = null)
        whenever(elasticSearchService.addOrUpdateFlightData(any())).thenReturn(responseWithNullDepartner)

        // When & Then
        mockMvc.perform(
            post("/api/v1/elastic-search/flight-data")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestWithNullDepartner))
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.flightId").value("AI101"))
            .andExpect(jsonPath("$.departner").isEmpty)
    }

    @Test
    fun `should handle request with null seat structure`() {
        // Given
        val requestWithNullSeats = sampleRequest.copy(seatStructure = null)
        val responseWithNullSeats = sampleResponse.copy(seatStructure = null)
        whenever(elasticSearchService.addOrUpdateFlightData(any())).thenReturn(responseWithNullSeats)

        // When & Then
        mockMvc.perform(
            post("/api/v1/elastic-search/flight-data")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestWithNullSeats))
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.flightId").value("AI101"))
            .andExpect(jsonPath("$.seatStructure").isEmpty)
    }

    @Test
    fun `should handle different maximum stops values`() {
        // Given
        val requestWithTwoStops = sampleRequest.copy(maximumStops = 2)
        val responseWithTwoStops = sampleResponse.copy(maximumStops = 2)
        whenever(elasticSearchService.addOrUpdateFlightData(any())).thenReturn(responseWithTwoStops)

        // When & Then
        mockMvc.perform(
            post("/api/v1/elastic-search/flight-data")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestWithTwoStops))
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.maximumStops").value(2))
    }

    @Test
    fun `should handle complex seat structure`() {
        // Given
        val complexSeatStructure = objectMapper.readTree("""
            {
                "economy": {
                    "total": 150,
                    "available": 120,
                    "price": 5000
                },
                "business": {
                    "total": 20,
                    "available": 15,
                    "price": 15000
                },
                "first": {
                    "total": 10,
                    "available": 8,
                    "price": 25000
                }
            }
        """)
        
        val requestWithComplexSeats = sampleRequest.copy(seatStructure = complexSeatStructure)
        val responseWithComplexSeats = sampleResponse.copy(seatStructure = complexSeatStructure)
        whenever(elasticSearchService.addOrUpdateFlightData(any())).thenReturn(responseWithComplexSeats)

        // When & Then
        mockMvc.perform(
            post("/api/v1/elastic-search/flight-data")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestWithComplexSeats))
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.seatStructure.economy.total").value(150))
            .andExpect(jsonPath("$.seatStructure.business.total").value(20))
            .andExpect(jsonPath("$.seatStructure.first.total").value(10))
    }
}
