package com.travel_search.travel_search_service.common.utils

import com.travel_search.travel_search_service.dto.FlightSearchRequest
import com.travel_search.travel_search_service.dto.SortBy
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate

class CacheKeyGeneratorTest {

    @Test
    fun `should generate consistent flight search keys for same request`() {
        // Given
        val request = FlightSearchRequest(
            src = "Delhi",
            destination = "Mumbai",
            startDate = LocalDate.of(2024, 1, 15),
            departner = listOf("Air India", "IndiGo"),
            maximumStops = listOf(0, 1),
            sortBy = SortBy.PRICE
        )

        // When
        val key1 = CacheKeyGenerator.generateFlightSearchKey(request)
        val key2 = CacheKeyGenerator.generateFlightSearchKey(request)

        // Then
        assertEquals(key1, key2)
        assertTrue(key1.startsWith("flight_search:"))
    }

    @Test
    fun `should generate different keys for different requests`() {
        // Given
        val request1 = FlightSearchRequest(
            src = "Delhi",
            destination = "Mumbai",
            startDate = LocalDate.of(2024, 1, 15),
            departner = listOf("Air India"),
            maximumStops = listOf(0),
            sortBy = SortBy.PRICE
        )
        
        val request2 = FlightSearchRequest(
            src = "Delhi",
            destination = "Bangalore",
            startDate = LocalDate.of(2024, 1, 15),
            departner = listOf("Air India"),
            maximumStops = listOf(0),
            sortBy = SortBy.PRICE
        )

        // When
        val key1 = CacheKeyGenerator.generateFlightSearchKey(request1)
        val key2 = CacheKeyGenerator.generateFlightSearchKey(request2)

        // Then
        assertNotEquals(key1, key2)
        assertTrue(key1.startsWith("flight_search:"))
        assertTrue(key2.startsWith("flight_search:"))
    }

    @Test
    fun `should generate same key regardless of departner list order`() {
        // Given
        val request1 = FlightSearchRequest(
            src = "Delhi",
            destination = "Mumbai",
            startDate = LocalDate.of(2024, 1, 15),
            departner = listOf("Air India", "IndiGo", "SpiceJet"),
            maximumStops = listOf(0, 1),
            sortBy = SortBy.PRICE
        )
        
        val request2 = FlightSearchRequest(
            src = "Delhi",
            destination = "Mumbai",
            startDate = LocalDate.of(2024, 1, 15),
            departner = listOf("SpiceJet", "Air India", "IndiGo"),
            maximumStops = listOf(0, 1),
            sortBy = SortBy.PRICE
        )

        // When
        val key1 = CacheKeyGenerator.generateFlightSearchKey(request1)
        val key2 = CacheKeyGenerator.generateFlightSearchKey(request2)

        // Then
        assertEquals(key1, key2)
    }

    @Test
    fun `should generate same key regardless of maximumStops list order`() {
        // Given
        val request1 = FlightSearchRequest(
            src = "Delhi",
            destination = "Mumbai",
            startDate = LocalDate.of(2024, 1, 15),
            departner = listOf("Air India"),
            maximumStops = listOf(2, 0, 1),
            sortBy = SortBy.PRICE
        )
        
        val request2 = FlightSearchRequest(
            src = "Delhi",
            destination = "Mumbai",
            startDate = LocalDate.of(2024, 1, 15),
            departner = listOf("Air India"),
            maximumStops = listOf(0, 1, 2),
            sortBy = SortBy.PRICE
        )

        // When
        val key1 = CacheKeyGenerator.generateFlightSearchKey(request1)
        val key2 = CacheKeyGenerator.generateFlightSearchKey(request2)

        // Then
        assertEquals(key1, key2)
    }

    @Test
    fun `should generate different keys for different sort options`() {
        // Given
        val baseRequest = FlightSearchRequest(
            src = "Delhi",
            destination = "Mumbai",
            startDate = LocalDate.of(2024, 1, 15),
            departner = listOf("Air India"),
            maximumStops = listOf(0),
            sortBy = SortBy.PRICE
        )

        // When
        val keyPrice = CacheKeyGenerator.generateFlightSearchKey(baseRequest)
        val keyTime = CacheKeyGenerator.generateFlightSearchKey(baseRequest.copy(sortBy = SortBy.TIME))
        val keyDuration = CacheKeyGenerator.generateFlightSearchKey(baseRequest.copy(sortBy = SortBy.DURATION))
        val keyStops = CacheKeyGenerator.generateFlightSearchKey(baseRequest.copy(sortBy = SortBy.STOPS))

        // Then
        assertNotEquals(keyPrice, keyTime)
        assertNotEquals(keyPrice, keyDuration)
        assertNotEquals(keyPrice, keyStops)
        assertNotEquals(keyTime, keyDuration)
        assertNotEquals(keyTime, keyStops)
        assertNotEquals(keyDuration, keyStops)
    }

    @Test
    fun `should handle empty departner list`() {
        // Given
        val request = FlightSearchRequest(
            src = "Delhi",
            destination = "Mumbai",
            startDate = LocalDate.of(2024, 1, 15),
            departner = emptyList(),
            maximumStops = listOf(0),
            sortBy = SortBy.PRICE
        )

        // When
        val key = CacheKeyGenerator.generateFlightSearchKey(request)

        // Then
        assertNotNull(key)
        assertTrue(key.startsWith("flight_search:"))
        assertTrue(key.length > "flight_search:".length)
    }

    @Test
    fun `should handle empty maximumStops list`() {
        // Given
        val request = FlightSearchRequest(
            src = "Delhi",
            destination = "Mumbai",
            startDate = LocalDate.of(2024, 1, 15),
            departner = listOf("Air India"),
            maximumStops = emptyList(),
            sortBy = SortBy.PRICE
        )

        // When
        val key = CacheKeyGenerator.generateFlightSearchKey(request)

        // Then
        assertNotNull(key)
        assertTrue(key.startsWith("flight_search:"))
    }

    @Test
    fun `should generate elastic search key correctly`() {
        // Given
        val flightId = "AI101"

        // When
        val key = CacheKeyGenerator.generateElasticSearchKey(flightId)

        // Then
        assertEquals("elastic_search:AI101", key)
    }

    @Test
    fun `should generate different elastic search keys for different flight IDs`() {
        // Given
        val flightId1 = "AI101"
        val flightId2 = "6E202"

        // When
        val key1 = CacheKeyGenerator.generateElasticSearchKey(flightId1)
        val key2 = CacheKeyGenerator.generateElasticSearchKey(flightId2)

        // Then
        assertEquals("elastic_search:AI101", key1)
        assertEquals("elastic_search:6E202", key2)
        assertNotEquals(key1, key2)
    }

    @Test
    fun `should handle special characters in flight ID`() {
        // Given
        val flightIdWithSpecialChars = "AI-101_TEST"

        // When
        val key = CacheKeyGenerator.generateElasticSearchKey(flightIdWithSpecialChars)

        // Then
        assertEquals("elastic_search:AI-101_TEST", key)
    }

    @Test
    fun `should generate consistent hash for same input`() {
        // Given
        val request = FlightSearchRequest(
            src = "Delhi",
            destination = "Mumbai",
            startDate = LocalDate.of(2024, 1, 15),
            departner = listOf("Air India"),
            maximumStops = listOf(0),
            sortBy = SortBy.PRICE
        )

        // When
        val key1 = CacheKeyGenerator.generateFlightSearchKey(request)
        val key2 = CacheKeyGenerator.generateFlightSearchKey(request)

        // Then
        assertEquals(key1, key2)
        // Verify the hash part is consistent (after "flight_search:")
        val hash1 = key1.substringAfter("flight_search:")
        val hash2 = key2.substringAfter("flight_search:")
        assertEquals(hash1, hash2)
        assertEquals(32, hash1.length) // MD5 hash should be 32 characters
    }

    @Test
    fun `should handle case sensitivity in source and destination`() {
        // Given
        val request1 = FlightSearchRequest(
            src = "delhi",
            destination = "mumbai",
            startDate = LocalDate.of(2024, 1, 15),
            departner = listOf("Air India"),
            maximumStops = listOf(0),
            sortBy = SortBy.PRICE
        )
        
        val request2 = FlightSearchRequest(
            src = "Delhi",
            destination = "Mumbai",
            startDate = LocalDate.of(2024, 1, 15),
            departner = listOf("Air India"),
            maximumStops = listOf(0),
            sortBy = SortBy.PRICE
        )

        // When
        val key1 = CacheKeyGenerator.generateFlightSearchKey(request1)
        val key2 = CacheKeyGenerator.generateFlightSearchKey(request2)

        // Then
        assertNotEquals(key1, key2) // Should be case sensitive
    }

    @Test
    fun `should handle different dates correctly`() {
        // Given
        val baseRequest = FlightSearchRequest(
            src = "Delhi",
            destination = "Mumbai",
            startDate = LocalDate.of(2024, 1, 15),
            departner = listOf("Air India"),
            maximumStops = listOf(0),
            sortBy = SortBy.PRICE
        )

        // When
        val key1 = CacheKeyGenerator.generateFlightSearchKey(baseRequest)
        val key2 = CacheKeyGenerator.generateFlightSearchKey(baseRequest.copy(startDate = LocalDate.of(2024, 1, 16)))

        // Then
        assertNotEquals(key1, key2)
    }
}
