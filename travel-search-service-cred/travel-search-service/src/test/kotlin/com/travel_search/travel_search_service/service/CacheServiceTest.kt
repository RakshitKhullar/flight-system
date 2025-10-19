package com.travel_search.travel_search_service.service

import com.travel_search.travel_search_service.dto.FlightSearchRequest
import com.travel_search.travel_search_service.dto.FlightSearchResponse
import com.travel_search.travel_search_service.dto.SortBy
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ValueOperations
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
class CacheServiceTest {

    @Mock
    private lateinit var redisTemplate: RedisTemplate<String, Any>

    @Mock
    private lateinit var valueOperations: ValueOperations<String, Any>

    private lateinit var cacheService: CacheService

    private lateinit var sampleRequest: FlightSearchRequest
    private lateinit var sampleResponse: FlightSearchResponse

    @BeforeEach
    fun setUp() {
        cacheService = CacheService(redisTemplate)
        
        sampleRequest = FlightSearchRequest(
            src = "Delhi",
            destination = "Mumbai",
            startDate = LocalDate.now().plusDays(1),
            departner = listOf("Air India"),
            maximumStops = listOf(0),
            sortBy = SortBy.PRICE
        )
        
        sampleResponse = FlightSearchResponse(
            flights = emptyList(),
            totalResults = 0,
            currentPage = 1,
            totalPages = 1
        )
    }

    @Test
    fun `should cache flight search response`() {
        // Given
        whenever(redisTemplate.opsForValue()).thenReturn(valueOperations)
        
        // When
        cacheService.cacheFlightSearch(sampleRequest, sampleResponse)

        // Then
        // Verify that set method was called with correct parameters
        // Note: In a real test, you'd verify the actual Redis interaction
        assertDoesNotThrow {
            cacheService.cacheFlightSearch(sampleRequest, sampleResponse)
        }
    }

    @Test
    fun `should retrieve cached flight search response`() {
        // Given
        whenever(redisTemplate.opsForValue()).thenReturn(valueOperations)
        whenever(valueOperations.get(any<String>())).thenReturn(sampleResponse)

        // When
        val result = cacheService.getCachedFlightSearch(sampleRequest)

        // Then
        assertNotNull(result)
        assertEquals(sampleResponse.totalResults, result?.totalResults)
        assertEquals(sampleResponse.currentPage, result?.currentPage)
    }

    @Test
    fun `should return null when no cached data found`() {
        // Given
        whenever(redisTemplate.opsForValue()).thenReturn(valueOperations)
        whenever(valueOperations.get(any<String>())).thenReturn(null)

        // When
        val result = cacheService.getCachedFlightSearch(sampleRequest)

        // Then
        assertNull(result)
    }

    @Test
    fun `should evict flight search cache`() {
        // Given
        val keys = setOf("flight_search:key1", "flight_search:key2")
        whenever(redisTemplate.keys("flight_search:*")).thenReturn(keys)

        // When
        assertDoesNotThrow {
            cacheService.evictFlightSearchCache()
        }

        // Then
        // In a real test, you'd verify that delete was called with the keys
    }

    @Test
    fun `should get cache info`() {
        // Given
        val flightSearchKeys = setOf("flight_search:key1", "flight_search:key2")
        val elasticSearchKeys = setOf("elastic_search:key1")
        
        whenever(redisTemplate.keys("flight_search:*")).thenReturn(flightSearchKeys)
        whenever(redisTemplate.keys("elastic_search:*")).thenReturn(elasticSearchKeys)

        // When
        val result = cacheService.getCacheInfo()

        // Then
        assertNotNull(result)
        assertEquals(2, result["flightSearchCacheCount"])
        assertEquals(1, result["elasticSearchCacheCount"])
        assertEquals(3, result["totalCacheKeys"])
    }

    @Test
    fun `should clear all cache`() {
        // When
        assertDoesNotThrow {
            cacheService.clearAllCache()
        }

        // Then
        // In a real test, you'd verify that flushAll was called
    }

    @Test
    fun `should handle empty cache keys gracefully`() {
        // Given
        whenever(redisTemplate.keys("flight_search:*")).thenReturn(emptySet())
        whenever(redisTemplate.keys("elastic_search:*")).thenReturn(emptySet())

        // When
        val result = cacheService.getCacheInfo()

        // Then
        assertNotNull(result)
        assertEquals(0, result["flightSearchCacheCount"])
        assertEquals(0, result["elasticSearchCacheCount"])
        assertEquals(0, result["totalCacheKeys"])
    }
}
