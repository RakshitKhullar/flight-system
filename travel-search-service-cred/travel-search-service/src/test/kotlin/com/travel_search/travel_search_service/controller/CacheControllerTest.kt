package com.travel_search.travel_search_service.controller

import com.travel_search.travel_search_service.service.CacheService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.context.junit.jupiter.SpringJUnitExtension
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@ExtendWith(SpringJUnitExtension::class)
@WebMvcTest(CacheController::class)
class CacheControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var cacheService: CacheService

    @BeforeEach
    fun setUp() {
        // Setup common mock behaviors
    }

    @Test
    fun `should get cache info successfully`() {
        // Given
        val cacheInfo = mapOf(
            "flightSearchCacheCount" to 5,
            "elasticSearchCacheCount" to 3,
            "totalCacheKeys" to 8
        )
        whenever(cacheService.getCacheInfo()).thenReturn(cacheInfo)

        // When & Then
        mockMvc.perform(
            get("/api/v1/cache/info")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.flightSearchCacheCount").value(5))
            .andExpect(jsonPath("$.elasticSearchCacheCount").value(3))
            .andExpect(jsonPath("$.totalCacheKeys").value(8))
    }

    @Test
    fun `should get cache info with zero counts`() {
        // Given
        val emptyCacheInfo = mapOf(
            "flightSearchCacheCount" to 0,
            "elasticSearchCacheCount" to 0,
            "totalCacheKeys" to 0
        )
        whenever(cacheService.getCacheInfo()).thenReturn(emptyCacheInfo)

        // When & Then
        mockMvc.perform(
            get("/api/v1/cache/info")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.flightSearchCacheCount").value(0))
            .andExpect(jsonPath("$.elasticSearchCacheCount").value(0))
            .andExpect(jsonPath("$.totalCacheKeys").value(0))
    }

    @Test
    fun `should evict flight search cache successfully`() {
        // When & Then
        mockMvc.perform(
            delete("/api/v1/cache/flight-search")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.message").value("Flight search cache cleared successfully"))
    }

    @Test
    fun `should clear all cache successfully`() {
        // When & Then
        mockMvc.perform(
            delete("/api/v1/cache/all")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.message").value("All cache cleared successfully"))
    }

    @Test
    fun `should handle cache info request when service returns large numbers`() {
        // Given
        val largeCacheInfo = mapOf(
            "flightSearchCacheCount" to 10000,
            "elasticSearchCacheCount" to 5000,
            "totalCacheKeys" to 15000
        )
        whenever(cacheService.getCacheInfo()).thenReturn(largeCacheInfo)

        // When & Then
        mockMvc.perform(
            get("/api/v1/cache/info")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.flightSearchCacheCount").value(10000))
            .andExpect(jsonPath("$.elasticSearchCacheCount").value(5000))
            .andExpect(jsonPath("$.totalCacheKeys").value(15000))
    }

    @Test
    fun `should handle multiple cache operations in sequence`() {
        // Given
        val initialCacheInfo = mapOf(
            "flightSearchCacheCount" to 5,
            "elasticSearchCacheCount" to 3,
            "totalCacheKeys" to 8
        )
        val emptyCacheInfo = mapOf(
            "flightSearchCacheCount" to 0,
            "elasticSearchCacheCount" to 0,
            "totalCacheKeys" to 0
        )
        
        whenever(cacheService.getCacheInfo())
            .thenReturn(initialCacheInfo)
            .thenReturn(emptyCacheInfo)

        // When & Then - Get initial cache info
        mockMvc.perform(
            get("/api/v1/cache/info")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalCacheKeys").value(8))

        // Clear flight search cache
        mockMvc.perform(
            delete("/api/v1/cache/flight-search")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.message").value("Flight search cache cleared successfully"))

        // Clear all cache
        mockMvc.perform(
            delete("/api/v1/cache/all")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.message").value("All cache cleared successfully"))
    }

    @Test
    fun `should return correct content type for all endpoints`() {
        // Given
        val cacheInfo = mapOf("totalCacheKeys" to 0)
        whenever(cacheService.getCacheInfo()).thenReturn(cacheInfo)

        // Test cache info endpoint
        mockMvc.perform(get("/api/v1/cache/info"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))

        // Test evict flight search cache endpoint
        mockMvc.perform(delete("/api/v1/cache/flight-search"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))

        // Test clear all cache endpoint
        mockMvc.perform(delete("/api/v1/cache/all"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
    }
}
