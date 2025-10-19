package com.travel_search.travel_search_service.common.utils

import com.travel_search.travel_search_service.dto.Flight
import com.travel_search.travel_search_service.dto.SortBy
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class FlightUtilsTest {

    private lateinit var sampleFlights: List<Flight>

    @BeforeEach
    fun setUp() {
        val baseTime = LocalDateTime.now().plusDays(1)
        
        sampleFlights = listOf(
            Flight(
                flightId = "AI101",
                airline = "Air India",
                flightNumber = "AI101",
                src = "Delhi",
                destination = "Mumbai",
                departureTime = baseTime.plusHours(2),
                arrivalTime = baseTime.plusHours(4),
                duration = "2h 0m",
                stops = 0,
                price = 8000.0,
                currency = "INR",
                availableSeats = 150,
                aircraftType = "Boeing 737",
                returnFlight = null
            ),
            Flight(
                flightId = "6E202",
                airline = "IndiGo",
                flightNumber = "6E202",
                src = "Delhi",
                destination = "Mumbai",
                departureTime = baseTime,
                arrivalTime = baseTime.plusHours(3),
                duration = "3h 0m",
                stops = 1,
                price = 5000.0,
                currency = "INR",
                availableSeats = 180,
                aircraftType = "Airbus A320",
                returnFlight = null
            ),
            Flight(
                flightId = "SG303",
                airline = "SpiceJet",
                flightNumber = "SG303",
                src = "Delhi",
                destination = "Mumbai",
                departureTime = baseTime.plusHours(4),
                arrivalTime = baseTime.plusHours(5).plusMinutes(30),
                duration = "1h 30m",
                stops = 2,
                price = 4500.0,
                currency = "INR",
                availableSeats = 120,
                aircraftType = "Boeing 737",
                returnFlight = null
            )
        )
    }

    @Test
    fun `should sort flights by price in ascending order`() {
        // When
        val result = FlightUtils.sortFlights(sampleFlights, SortBy.PRICE)

        // Then
        assertEquals(3, result.size)
        assertEquals(4500.0, result[0].price) // SpiceJet - cheapest
        assertEquals(5000.0, result[1].price) // IndiGo - middle
        assertEquals(8000.0, result[2].price) // Air India - most expensive
    }

    @Test
    fun `should sort flights by departure time in ascending order`() {
        // When
        val result = FlightUtils.sortFlights(sampleFlights, SortBy.TIME)

        // Then
        assertEquals(3, result.size)
        assertEquals("6E202", result[0].flightId) // IndiGo - earliest
        assertEquals("AI101", result[1].flightId) // Air India - middle
        assertEquals("SG303", result[2].flightId) // SpiceJet - latest
    }

    @Test
    fun `should sort flights by duration in ascending order`() {
        // When
        val result = FlightUtils.sortFlights(sampleFlights, SortBy.DURATION)

        // Then
        assertEquals(3, result.size)
        assertEquals("1h 30m", result[0].duration) // SpiceJet - shortest
        assertEquals("2h 0m", result[1].duration) // Air India - middle
        assertEquals("3h 0m", result[2].duration) // IndiGo - longest
    }

    @Test
    fun `should sort flights by stops in ascending order`() {
        // When
        val result = FlightUtils.sortFlights(sampleFlights, SortBy.STOPS)

        // Then
        assertEquals(3, result.size)
        assertEquals(0, result[0].stops) // Air India - non-stop
        assertEquals(1, result[1].stops) // IndiGo - 1 stop
        assertEquals(2, result[2].stops) // SpiceJet - 2 stops
    }

    @Test
    fun `should handle empty flight list`() {
        // Given
        val emptyFlights = emptyList<Flight>()

        // When
        val result = FlightUtils.sortFlights(emptyFlights, SortBy.PRICE)

        // Then
        assertTrue(result.isEmpty())
    }

    @Test
    fun `should handle single flight list`() {
        // Given
        val singleFlight = listOf(sampleFlights[0])

        // When
        val result = FlightUtils.sortFlights(singleFlight, SortBy.PRICE)

        // Then
        assertEquals(1, result.size)
        assertEquals(sampleFlights[0], result[0])
    }

    @Test
    fun `should handle flights with same price`() {
        // Given
        val flightsWithSamePrice = listOf(
            sampleFlights[0].copy(price = 5000.0),
            sampleFlights[1].copy(price = 5000.0),
            sampleFlights[2].copy(price = 5000.0)
        )

        // When
        val result = FlightUtils.sortFlights(flightsWithSamePrice, SortBy.PRICE)

        // Then
        assertEquals(3, result.size)
        // All flights should have the same price
        assertTrue(result.all { it.price == 5000.0 })
    }

    @Test
    fun `should handle flights with same departure time`() {
        // Given
        val sameTime = LocalDateTime.now().plusDays(1)
        val flightsWithSameTime = listOf(
            sampleFlights[0].copy(departureTime = sameTime),
            sampleFlights[1].copy(departureTime = sameTime),
            sampleFlights[2].copy(departureTime = sameTime)
        )

        // When
        val result = FlightUtils.sortFlights(flightsWithSameTime, SortBy.TIME)

        // Then
        assertEquals(3, result.size)
        // All flights should have the same departure time
        assertTrue(result.all { it.departureTime == sameTime })
    }

    @Test
    fun `should parse duration correctly for various formats`() {
        // Given
        val flightsWithVariousDurations = listOf(
            sampleFlights[0].copy(duration = "10h 45m"),
            sampleFlights[1].copy(duration = "2h 15m"),
            sampleFlights[2].copy(duration = "1h 0m")
        )

        // When
        val result = FlightUtils.sortFlights(flightsWithVariousDurations, SortBy.DURATION)

        // Then
        assertEquals("1h 0m", result[0].duration) // 60 minutes
        assertEquals("2h 15m", result[1].duration) // 135 minutes
        assertEquals("10h 45m", result[2].duration) // 645 minutes
    }

    @Test
    fun `should handle malformed duration strings gracefully`() {
        // Given
        val flightsWithMalformedDurations = listOf(
            sampleFlights[0].copy(duration = "invalid"),
            sampleFlights[1].copy(duration = "2h 15m"),
            sampleFlights[2].copy(duration = "")
        )

        // When & Then - Should not throw exception
        assertDoesNotThrow {
            val result = FlightUtils.sortFlights(flightsWithMalformedDurations, SortBy.DURATION)
            assertEquals(3, result.size)
        }
    }

    @Test
    fun `should handle duration with only hours`() {
        // Given
        val flightsWithHoursOnly = listOf(
            sampleFlights[0].copy(duration = "3h"),
            sampleFlights[1].copy(duration = "1h"),
            sampleFlights[2].copy(duration = "2h")
        )

        // When
        val result = FlightUtils.sortFlights(flightsWithHoursOnly, SortBy.DURATION)

        // Then
        assertEquals("1h", result[0].duration)
        assertEquals("2h", result[1].duration)
        assertEquals("3h", result[2].duration)
    }

    @Test
    fun `should handle duration with only minutes`() {
        // Given
        val flightsWithMinutesOnly = listOf(
            sampleFlights[0].copy(duration = "90m"),
            sampleFlights[1].copy(duration = "45m"),
            sampleFlights[2].copy(duration = "120m")
        )

        // When
        val result = FlightUtils.sortFlights(flightsWithMinutesOnly, SortBy.DURATION)

        // Then
        assertEquals("45m", result[0].duration)
        assertEquals("90m", result[1].duration)
        assertEquals("120m", result[2].duration)
    }

    @Test
    fun `should maintain stable sort for equal values`() {
        // Given - flights with same stops but different flight IDs
        val flightsWithSameStops = listOf(
            sampleFlights[0].copy(stops = 1, flightId = "FIRST"),
            sampleFlights[1].copy(stops = 1, flightId = "SECOND"),
            sampleFlights[2].copy(stops = 1, flightId = "THIRD")
        )

        // When
        val result = FlightUtils.sortFlights(flightsWithSameStops, SortBy.STOPS)

        // Then - Order should be maintained for equal values
        assertEquals(3, result.size)
        assertEquals("FIRST", result[0].flightId)
        assertEquals("SECOND", result[1].flightId)
        assertEquals("THIRD", result[2].flightId)
    }
}
