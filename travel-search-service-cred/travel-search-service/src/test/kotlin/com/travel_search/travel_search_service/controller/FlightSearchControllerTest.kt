package com.travel_search.travel_search_service.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.travel_search.travel_search_service.dto.Flight
import com.travel_search.travel_search_service.dto.FlightSearchResponse
import com.travel_search.travel_search_service.service.interfaces.IFlightSearchService
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.LocalDate
import java.time.LocalDateTime

@ExtendWith(SpringJUnitExtension::class)
@WebMvcTest(FlightSearchController::class)
class FlightSearchControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var flightSearchService: IFlightSearchService

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    private lateinit var sampleFlightResponse: FlightSearchResponse

    @BeforeEach
    fun setUp() {
        val sampleFlight = Flight(
            flightId = "AI101",
            airline = "Air India",
            flightNumber = "AI101",
            src = "Delhi",
            destination = "Mumbai",
            departureTime = LocalDateTime.now().plusDays(1),
            arrivalTime = LocalDateTime.now().plusDays(1).plusHours(2),
            duration = "2h 0m",
            stops = 0,
            price = 5000.0,
            currency = "INR",
            availableSeats = 150,
            aircraftType = "Boeing 737",
            returnFlight = null
        )

        sampleFlightResponse = FlightSearchResponse(
            flights = listOf(sampleFlight),
            totalResults = 1,
            currentPage = 1,
            totalPages = 1
        )
    }

    @Test
    fun `should return flight search results for valid request`() {
        // Given
        whenever(flightSearchService.searchFlights(any())).thenReturn(sampleFlightResponse)

        // When & Then
        mockMvc.perform(
            get("/api/v1/search/flights")
                .param("source", "Delhi")
                .param("destination", "Mumbai")
                .param("flightDate", LocalDate.now().plusDays(1).toString())
                .param("sortBy", "price")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.totalResults").value(1))
            .andExpect(jsonPath("$.flights").isArray)
            .andExpect(jsonPath("$.flights[0].flightId").value("AI101"))
            .andExpect(jsonPath("$.flights[0].src").value("Delhi"))
            .andExpect(jsonPath("$.flights[0].destination").value("Mumbai"))
    }

    @Test
    fun `should return empty results when no flights found`() {
        // Given
        val emptyResponse = FlightSearchResponse(
            flights = emptyList(),
            totalResults = 0,
            currentPage = 1,
            totalPages = 1
        )
        whenever(flightSearchService.searchFlights(any())).thenReturn(emptyResponse)

        // When & Then
        mockMvc.perform(
            get("/api/v1/search/flights")
                .param("source", "Delhi")
                .param("destination", "Chennai")
                .param("flightDate", LocalDate.now().plusDays(1).toString())
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.totalResults").value(0))
            .andExpect(jsonPath("$.flights").isEmpty)
    }

    @Test
    fun `should handle request with optional parameters`() {
        // Given
        whenever(flightSearchService.searchFlights(any())).thenReturn(sampleFlightResponse)

        // When & Then
        mockMvc.perform(
            get("/api/v1/search/flights")
                .param("source", "Delhi")
                .param("destination", "Mumbai")
                .param("flightDate", LocalDate.now().plusDays(1).toString())
                .param("departner", "Air India", "IndiGo")
                .param("maximumStops", "0", "1")
                .param("sortBy", "time")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.totalResults").value(1))
    }

    @Test
    fun `should handle request with default values for optional parameters`() {
        // Given
        whenever(flightSearchService.searchFlights(any())).thenReturn(sampleFlightResponse)

        // When & Then
        mockMvc.perform(
            get("/api/v1/search/flights")
                .param("source", "Delhi")
                .param("destination", "Mumbai")
                .param("flightDate", LocalDate.now().plusDays(1).toString())
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
    }

    @Test
    fun `should return bad request when required parameters are missing`() {
        // When & Then
        mockMvc.perform(
            get("/api/v1/search/flights")
                .param("source", "Delhi")
                // Missing destination and flightDate
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `should handle different sort options`() {
        // Given
        whenever(flightSearchService.searchFlights(any())).thenReturn(sampleFlightResponse)

        val sortOptions = listOf("price", "time", "duration", "stops")

        for (sortBy in sortOptions) {
            // When & Then
            mockMvc.perform(
                get("/api/v1/search/flights")
                    .param("source", "Delhi")
                    .param("destination", "Mumbai")
                    .param("flightDate", LocalDate.now().plusDays(1).toString())
                    .param("sortBy", sortBy)
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        }
    }

    @Test
    fun `should handle invalid sort option gracefully`() {
        // Given
        whenever(flightSearchService.searchFlights(any())).thenReturn(sampleFlightResponse)

        // When & Then
        mockMvc.perform(
            get("/api/v1/search/flights")
                .param("source", "Delhi")
                .param("destination", "Mumbai")
                .param("flightDate", LocalDate.now().plusDays(1).toString())
                .param("sortBy", "invalid_sort")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk) // Should default to price sorting
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
    }

    @Test
    fun `should handle multiple airlines in departner parameter`() {
        // Given
        whenever(flightSearchService.searchFlights(any())).thenReturn(sampleFlightResponse)

        // When & Then
        mockMvc.perform(
            get("/api/v1/search/flights")
                .param("source", "Delhi")
                .param("destination", "Mumbai")
                .param("flightDate", LocalDate.now().plusDays(1).toString())
                .param("departner", "Air India")
                .param("departner", "IndiGo")
                .param("departner", "SpiceJet")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
    }

    @Test
    fun `should handle multiple maximum stops`() {
        // Given
        whenever(flightSearchService.searchFlights(any())).thenReturn(sampleFlightResponse)

        // When & Then
        mockMvc.perform(
            get("/api/v1/search/flights")
                .param("source", "Delhi")
                .param("destination", "Mumbai")
                .param("flightDate", LocalDate.now().plusDays(1).toString())
                .param("maximumStops", "0")
                .param("maximumStops", "1")
                .param("maximumStops", "2")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
    }
}
