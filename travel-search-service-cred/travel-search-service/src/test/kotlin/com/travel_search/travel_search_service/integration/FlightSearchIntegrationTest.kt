package com.travel_search.travel_search_service.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.travel_search.travel_search_service.config.TestConfig
import com.travel_search.travel_search_service.dto.ElasticSearchRequest
import com.travel_search.travel_search_service.repository.ElasticSearchRepository
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.context.WebApplicationContext
import redis.embedded.RedisServer
import java.time.LocalDate

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
@ActiveProfiles("test")
@ContextConfiguration(classes = [TestConfig::class])
@Transactional
class FlightSearchIntegrationTest {

    @Autowired
    private lateinit var webApplicationContext: WebApplicationContext

    @Autowired
    private lateinit var elasticSearchRepository: ElasticSearchRepository

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var redisServer: RedisServer

    private lateinit var mockMvc: MockMvc

    @PostConstruct
    fun startRedis() {
        if (!redisServer.isActive) {
            redisServer.start()
        }
    }

    @PreDestroy
    fun stopRedis() {
        if (redisServer.isActive) {
            redisServer.stop()
        }
    }

    @BeforeEach
    fun setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
        elasticSearchRepository.deleteAll()
    }

    @Test
    fun `should complete full flight search workflow`() {
        // Step 1: Add flight data
        val seatStructure = objectMapper.readTree("""{"economy": 150, "business": 20, "first": 10}""")
        val flightRequest = ElasticSearchRequest(
            flightId = "AI101",
            source = "Delhi",
            destination = "Mumbai",
            flightDate = LocalDate.now().plusDays(1),
            maximumStops = 0,
            departner = "Air India",
            seatStructure = seatStructure
        )

        // Add flight data
        mockMvc.perform(
            post("/api/v1/elastic-search/flight-data")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(flightRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.flightId").value("AI101"))
            .andExpect(jsonPath("$.isNewEntry").value(true))

        // Step 2: Search for flights
        mockMvc.perform(
            get("/api/v1/search/flights")
                .param("source", "Delhi")
                .param("destination", "Mumbai")
                .param("flightDate", LocalDate.now().plusDays(1).toString())
                .param("sortBy", "price")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalResults").value(1))
            .andExpect(jsonPath("$.flights[0].flightId").value("AI101"))
            .andExpect(jsonPath("$.flights[0].src").value("Delhi"))
            .andExpect(jsonPath("$.flights[0].destination").value("Mumbai"))

        // Step 3: Get flight by ID
        mockMvc.perform(
            get("/api/v1/elastic-search/flight/AI101")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].flightId").value("AI101"))
    }

    @Test
    fun `should handle cache operations correctly`() {
        // Step 1: Get initial cache info
        mockMvc.perform(get("/api/v1/cache/info"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalCacheKeys").exists())

        // Step 2: Add flight data (this should clear flight search cache)
        val flightRequest = ElasticSearchRequest(
            flightId = "6E202",
            source = "Bangalore",
            destination = "Chennai",
            flightDate = LocalDate.now().plusDays(2),
            maximumStops = 1,
            departner = "IndiGo",
            seatStructure = null
        )

        mockMvc.perform(
            post("/api/v1/elastic-search/flight-data")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(flightRequest))
        )
            .andExpect(status().isOk)

        // Step 3: Search flights (should cache the result)
        mockMvc.perform(
            get("/api/v1/search/flights")
                .param("source", "Bangalore")
                .param("destination", "Chennai")
                .param("flightDate", LocalDate.now().plusDays(2).toString())
        )
            .andExpect(status().isOk)

        // Step 4: Search same flights again (should hit cache)
        mockMvc.perform(
            get("/api/v1/search/flights")
                .param("source", "Bangalore")
                .param("destination", "Chennai")
                .param("flightDate", LocalDate.now().plusDays(2).toString())
        )
            .andExpect(status().isOk)

        // Step 5: Clear flight search cache
        mockMvc.perform(delete("/api/v1/cache/flight-search"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.message").value("Flight search cache cleared successfully"))

        // Step 6: Clear all cache
        mockMvc.perform(delete("/api/v1/cache/all"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.message").value("All cache cleared successfully"))
    }

    @Test
    fun `should handle flight data updates correctly`() {
        // Step 1: Add initial flight data
        val initialSeatStructure = objectMapper.readTree("""{"economy": 150, "business": 20}""")
        val initialRequest = ElasticSearchRequest(
            flightId = "SG303",
            source = "Delhi",
            destination = "Goa",
            flightDate = LocalDate.now().plusDays(3),
            maximumStops = 1,
            departner = "SpiceJet",
            seatStructure = initialSeatStructure
        )

        mockMvc.perform(
            post("/api/v1/elastic-search/flight-data")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(initialRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.isNewEntry").value(true))

        // Step 2: Update seat structure for same flight
        val updatedSeatStructure = objectMapper.readTree("""{"economy": 140, "business": 25, "first": 5}""")
        val updateRequest = initialRequest.copy(seatStructure = updatedSeatStructure)

        mockMvc.perform(
            post("/api/v1/elastic-search/flight-data")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.isNewEntry").value(false))
            .andExpect(jsonPath("$.message").value("Seat structure updated successfully"))

        // Step 3: Add different route for same flight ID
        val differentRouteRequest = initialRequest.copy(
            destination = "Pune",
            seatStructure = initialSeatStructure
        )

        mockMvc.perform(
            post("/api/v1/elastic-search/flight-data")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(differentRouteRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.isNewEntry").value(true))

        // Step 4: Verify multiple entries for same flight ID
        mockMvc.perform(get("/api/v1/elastic-search/flight/SG303"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
    }

    @Test
    fun `should handle different sorting options`() {
        // Add multiple flights
        val flights = listOf(
            ElasticSearchRequest(
                flightId = "AI101",
                source = "Delhi",
                destination = "Mumbai",
                flightDate = LocalDate.now().plusDays(1),
                maximumStops = 0,
                departner = "Air India",
                seatStructure = null
            ),
            ElasticSearchRequest(
                flightId = "6E202",
                source = "Delhi",
                destination = "Mumbai",
                flightDate = LocalDate.now().plusDays(1),
                maximumStops = 1,
                departner = "IndiGo",
                seatStructure = null
            ),
            ElasticSearchRequest(
                flightId = "SG303",
                source = "Delhi",
                destination = "Mumbai",
                flightDate = LocalDate.now().plusDays(1),
                maximumStops = 2,
                departner = "SpiceJet",
                seatStructure = null
            )
        )

        // Add all flights
        flights.forEach { flight ->
            mockMvc.perform(
                post("/api/v1/elastic-search/flight-data")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(flight))
            )
                .andExpect(status().isOk)
        }

        val sortOptions = listOf("price", "time", "duration", "stops")
        val flightDate = LocalDate.now().plusDays(1).toString()

        // Test each sorting option
        sortOptions.forEach { sortBy ->
            mockMvc.perform(
                get("/api/v1/search/flights")
                    .param("source", "Delhi")
                    .param("destination", "Mumbai")
                    .param("flightDate", flightDate)
                    .param("sortBy", sortBy)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.totalResults").value(3))
                .andExpect(jsonPath("$.flights").isArray)
        }
    }

    @Test
    fun `should handle validation errors correctly`() {
        // Test invalid flight search request
        mockMvc.perform(
            get("/api/v1/search/flights")
                .param("source", "")
                .param("destination", "Mumbai")
                .param("flightDate", LocalDate.now().plusDays(1).toString())
        )
            .andExpect(status().isBadRequest)

        // Test invalid elastic search request
        val invalidRequest = ElasticSearchRequest(
            flightId = "",
            source = "Delhi",
            destination = "Mumbai",
            flightDate = LocalDate.now().plusDays(1),
            maximumStops = 0,
            departner = "Air India",
            seatStructure = null
        )

        mockMvc.perform(
            post("/api/v1/elastic-search/flight-data")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `should handle non-existent flight ID correctly`() {
        mockMvc.perform(get("/api/v1/elastic-search/flight/NONEXISTENT"))
            .andExpect(status().isNotFound)
    }
}
