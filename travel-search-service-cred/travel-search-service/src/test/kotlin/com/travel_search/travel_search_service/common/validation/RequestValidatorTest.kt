package com.travel_search.travel_search_service.common.validation

import com.fasterxml.jackson.databind.ObjectMapper
import com.travel_search.travel_search_service.dto.ElasticSearchRequest
import com.travel_search.travel_search_service.dto.FlightSearchRequest
import com.travel_search.travel_search_service.dto.SortBy
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate

class RequestValidatorTest {

    private val objectMapper = ObjectMapper()

    @Test
    fun `should validate valid flight search request successfully`() {
        // Given
        val validRequest = FlightSearchRequest(
            src = "Delhi",
            destination = "Mumbai",
            startDate = LocalDate.now().plusDays(1),
            departner = listOf("Air India"),
            maximumStops = listOf(0, 1),
            sortBy = SortBy.PRICE
        )

        // When & Then
        assertDoesNotThrow {
            RequestValidator.validateFlightSearchRequest(validRequest)
        }
    }

    @Test
    fun `should throw exception when source is blank in flight search request`() {
        // Given
        val invalidRequest = FlightSearchRequest(
            src = "",
            destination = "Mumbai",
            startDate = LocalDate.now().plusDays(1),
            departner = listOf("Air India"),
            maximumStops = listOf(0),
            sortBy = SortBy.PRICE
        )

        // When & Then
        val exception = assertThrows(IllegalArgumentException::class.java) {
            RequestValidator.validateFlightSearchRequest(invalidRequest)
        }
        assertEquals("Source cannot be blank", exception.message)
    }

    @Test
    fun `should throw exception when source is whitespace only in flight search request`() {
        // Given
        val invalidRequest = FlightSearchRequest(
            src = "   ",
            destination = "Mumbai",
            startDate = LocalDate.now().plusDays(1),
            departner = listOf("Air India"),
            maximumStops = listOf(0),
            sortBy = SortBy.PRICE
        )

        // When & Then
        val exception = assertThrows(IllegalArgumentException::class.java) {
            RequestValidator.validateFlightSearchRequest(invalidRequest)
        }
        assertEquals("Source cannot be blank", exception.message)
    }

    @Test
    fun `should throw exception when destination is blank in flight search request`() {
        // Given
        val invalidRequest = FlightSearchRequest(
            src = "Delhi",
            destination = "",
            startDate = LocalDate.now().plusDays(1),
            departner = listOf("Air India"),
            maximumStops = listOf(0),
            sortBy = SortBy.PRICE
        )

        // When & Then
        val exception = assertThrows(IllegalArgumentException::class.java) {
            RequestValidator.validateFlightSearchRequest(invalidRequest)
        }
        assertEquals("Destination cannot be blank", exception.message)
    }

    @Test
    fun `should throw exception when flight date is in past`() {
        // Given
        val invalidRequest = FlightSearchRequest(
            src = "Delhi",
            destination = "Mumbai",
            startDate = LocalDate.now().minusDays(1),
            departner = listOf("Air India"),
            maximumStops = listOf(0),
            sortBy = SortBy.PRICE
        )

        // When & Then
        val exception = assertThrows(IllegalArgumentException::class.java) {
            RequestValidator.validateFlightSearchRequest(invalidRequest)
        }
        assertEquals("Flight date must be today or in the future", exception.message)
    }

    @Test
    fun `should allow flight date to be today`() {
        // Given
        val validRequest = FlightSearchRequest(
            src = "Delhi",
            destination = "Mumbai",
            startDate = LocalDate.now(),
            departner = listOf("Air India"),
            maximumStops = listOf(0),
            sortBy = SortBy.PRICE
        )

        // When & Then
        assertDoesNotThrow {
            RequestValidator.validateFlightSearchRequest(validRequest)
        }
    }

    @Test
    fun `should throw exception when maximum stops contains negative values`() {
        // Given
        val invalidRequest = FlightSearchRequest(
            src = "Delhi",
            destination = "Mumbai",
            startDate = LocalDate.now().plusDays(1),
            departner = listOf("Air India"),
            maximumStops = listOf(0, -1, 1),
            sortBy = SortBy.PRICE
        )

        // When & Then
        val exception = assertThrows(IllegalArgumentException::class.java) {
            RequestValidator.validateFlightSearchRequest(invalidRequest)
        }
        assertEquals("Maximum stops cannot be negative", exception.message)
    }

    @Test
    fun `should validate valid elastic search request successfully`() {
        // Given
        val seatStructure = objectMapper.readTree("""{"economy": 150, "business": 20}""")
        val validRequest = ElasticSearchRequest(
            flightId = "AI101",
            source = "Delhi",
            destination = "Mumbai",
            flightDate = LocalDate.now().plusDays(1),
            maximumStops = 1,
            departner = "Air India",
            seatStructure = seatStructure
        )

        // When & Then
        assertDoesNotThrow {
            RequestValidator.validateElasticSearchRequest(validRequest)
        }
    }

    @Test
    fun `should throw exception when flight ID is blank in elastic search request`() {
        // Given
        val invalidRequest = ElasticSearchRequest(
            flightId = "",
            source = "Delhi",
            destination = "Mumbai",
            flightDate = LocalDate.now().plusDays(1),
            maximumStops = 0,
            departner = "Air India",
            seatStructure = null
        )

        // When & Then
        val exception = assertThrows(IllegalArgumentException::class.java) {
            RequestValidator.validateElasticSearchRequest(invalidRequest)
        }
        assertEquals("Flight ID cannot be blank", exception.message)
    }

    @Test
    fun `should throw exception when source is blank in elastic search request`() {
        // Given
        val invalidRequest = ElasticSearchRequest(
            flightId = "AI101",
            source = "",
            destination = "Mumbai",
            flightDate = LocalDate.now().plusDays(1),
            maximumStops = 0,
            departner = "Air India",
            seatStructure = null
        )

        // When & Then
        val exception = assertThrows(IllegalArgumentException::class.java) {
            RequestValidator.validateElasticSearchRequest(invalidRequest)
        }
        assertEquals("Source cannot be blank", exception.message)
    }

    @Test
    fun `should throw exception when destination is blank in elastic search request`() {
        // Given
        val invalidRequest = ElasticSearchRequest(
            flightId = "AI101",
            source = "Delhi",
            destination = "",
            flightDate = LocalDate.now().plusDays(1),
            maximumStops = 0,
            departner = "Air India",
            seatStructure = null
        )

        // When & Then
        val exception = assertThrows(IllegalArgumentException::class.java) {
            RequestValidator.validateElasticSearchRequest(invalidRequest)
        }
        assertEquals("Destination cannot be blank", exception.message)
    }

    @Test
    fun `should throw exception when maximum stops is negative in elastic search request`() {
        // Given
        val invalidRequest = ElasticSearchRequest(
            flightId = "AI101",
            source = "Delhi",
            destination = "Mumbai",
            flightDate = LocalDate.now().plusDays(1),
            maximumStops = -1,
            departner = "Air India",
            seatStructure = null
        )

        // When & Then
        val exception = assertThrows(IllegalArgumentException::class.java) {
            RequestValidator.validateElasticSearchRequest(invalidRequest)
        }
        assertEquals("Maximum stops cannot be negative", exception.message)
    }

    @Test
    fun `should throw exception when maximum stops exceeds 2 in elastic search request`() {
        // Given
        val invalidRequest = ElasticSearchRequest(
            flightId = "AI101",
            source = "Delhi",
            destination = "Mumbai",
            flightDate = LocalDate.now().plusDays(1),
            maximumStops = 3,
            departner = "Air India",
            seatStructure = null
        )

        // When & Then
        val exception = assertThrows(IllegalArgumentException::class.java) {
            RequestValidator.validateElasticSearchRequest(invalidRequest)
        }
        assertEquals("Maximum stops cannot exceed 2", exception.message)
    }

    @Test
    fun `should allow maximum stops of 0, 1, and 2 in elastic search request`() {
        // Given
        val baseRequest = ElasticSearchRequest(
            flightId = "AI101",
            source = "Delhi",
            destination = "Mumbai",
            flightDate = LocalDate.now().plusDays(1),
            maximumStops = 0,
            departner = "Air India",
            seatStructure = null
        )

        // When & Then
        assertDoesNotThrow {
            RequestValidator.validateElasticSearchRequest(baseRequest.copy(maximumStops = 0))
            RequestValidator.validateElasticSearchRequest(baseRequest.copy(maximumStops = 1))
            RequestValidator.validateElasticSearchRequest(baseRequest.copy(maximumStops = 2))
        }
    }

    @Test
    fun `should handle null departner in elastic search request`() {
        // Given
        val validRequest = ElasticSearchRequest(
            flightId = "AI101",
            source = "Delhi",
            destination = "Mumbai",
            flightDate = LocalDate.now().plusDays(1),
            maximumStops = 0,
            departner = null,
            seatStructure = null
        )

        // When & Then
        assertDoesNotThrow {
            RequestValidator.validateElasticSearchRequest(validRequest)
        }
    }

    @Test
    fun `should handle null seat structure in elastic search request`() {
        // Given
        val validRequest = ElasticSearchRequest(
            flightId = "AI101",
            source = "Delhi",
            destination = "Mumbai",
            flightDate = LocalDate.now().plusDays(1),
            maximumStops = 0,
            departner = "Air India",
            seatStructure = null
        )

        // When & Then
        assertDoesNotThrow {
            RequestValidator.validateElasticSearchRequest(validRequest)
        }
    }

    @Test
    fun `should handle empty departner list in flight search request`() {
        // Given
        val validRequest = FlightSearchRequest(
            src = "Delhi",
            destination = "Mumbai",
            startDate = LocalDate.now().plusDays(1),
            departner = emptyList(),
            maximumStops = listOf(0),
            sortBy = SortBy.PRICE
        )

        // When & Then
        assertDoesNotThrow {
            RequestValidator.validateFlightSearchRequest(validRequest)
        }
    }

    @Test
    fun `should handle empty maximum stops list in flight search request`() {
        // Given
        val validRequest = FlightSearchRequest(
            src = "Delhi",
            destination = "Mumbai",
            startDate = LocalDate.now().plusDays(1),
            departner = listOf("Air India"),
            maximumStops = emptyList(),
            sortBy = SortBy.PRICE
        )

        // When & Then
        assertDoesNotThrow {
            RequestValidator.validateFlightSearchRequest(validRequest)
        }
    }

    @Test
    fun `should validate flight search request with multiple valid maximum stops`() {
        // Given
        val validRequest = FlightSearchRequest(
            src = "Delhi",
            destination = "Mumbai",
            startDate = LocalDate.now().plusDays(1),
            departner = listOf("Air India"),
            maximumStops = listOf(0, 1, 2),
            sortBy = SortBy.PRICE
        )

        // When & Then
        assertDoesNotThrow {
            RequestValidator.validateFlightSearchRequest(validRequest)
        }
    }

    @Test
    fun `should handle mixed case in source and destination`() {
        // Given
        val validRequest = FlightSearchRequest(
            src = "dElHi",
            destination = "mUmBaI",
            startDate = LocalDate.now().plusDays(1),
            departner = listOf("Air India"),
            maximumStops = listOf(0),
            sortBy = SortBy.PRICE
        )

        // When & Then
        assertDoesNotThrow {
            RequestValidator.validateFlightSearchRequest(validRequest)
        }
    }
}
