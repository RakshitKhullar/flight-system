package com.travel_search.travel_search_service.common.mapper

import com.fasterxml.jackson.databind.ObjectMapper
import com.travel_search.travel_search_service.dto.ElasticSearchRequest
import com.travel_search.travel_search_service.repository.entity.ElasticSearch
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import java.time.LocalDate
import java.time.ZonedDateTime

@ExtendWith(MockitoExtension::class)
class EntityMapperTest {

    private lateinit var sampleRequest: ElasticSearchRequest
    private lateinit var sampleEntity: ElasticSearch
    private val objectMapper = ObjectMapper()

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

        sampleEntity = ElasticSearch(
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
    }

    @Test
    fun `should convert request to entity correctly`() {
        // When
        val result = EntityMapper.toEntity(sampleRequest)

        // Then
        assertNotNull(result)
        assertEquals(sampleRequest.flightId, result.flightId)
        assertEquals(sampleRequest.source, result.source)
        assertEquals(sampleRequest.destination, result.destination)
        assertEquals(sampleRequest.flightDate, result.flightDate)
        assertEquals(sampleRequest.maximumStops, result.maximumStops)
        assertEquals(sampleRequest.departner, result.departner)
        assertEquals(sampleRequest.seatStructure, result.seatStructure)
        assertNotNull(result.createdAt)
        assertNotNull(result.updatedAt)
    }

    @Test
    fun `should convert entity to response correctly`() {
        // Given
        val message = "Test message"
        val isNewEntry = true

        // When
        val result = EntityMapper.toResponse(sampleEntity, message, isNewEntry)

        // Then
        assertNotNull(result)
        assertEquals(sampleEntity.id, result.id)
        assertEquals(sampleEntity.flightId, result.flightId)
        assertEquals(sampleEntity.source, result.source)
        assertEquals(sampleEntity.destination, result.destination)
        assertEquals(sampleEntity.flightDate, result.flightDate)
        assertEquals(sampleEntity.maximumStops, result.maximumStops)
        assertEquals(sampleEntity.departner, result.departner)
        assertEquals(sampleEntity.seatStructure, result.seatStructure)
        assertEquals(message, result.message)
        assertEquals(isNewEntry, result.isNewEntry)
    }

    @Test
    fun `should convert entity to flight DTO correctly`() {
        // When
        val result = EntityMapper.toFlightDto(sampleEntity)

        // Then
        assertNotNull(result)
        assertEquals(sampleEntity.flightId, result.flightId)
        assertEquals(sampleEntity.source, result.src)
        assertEquals(sampleEntity.destination, result.destination)
        assertEquals(sampleEntity.maximumStops, result.stops)
        assertEquals(sampleEntity.departner, result.airline)
        assertEquals("INR", result.currency)
        assertEquals("Boeing 737", result.aircraftType)
        assertNull(result.returnFlight)
        
        // Verify generated fields
        assertTrue(result.price > 0)
        assertTrue(result.availableSeats in 10..180)
        assertNotNull(result.departureTime)
        assertNotNull(result.arrivalTime)
        assertNotNull(result.duration)
        assertNotNull(result.flightNumber)
        assertTrue(result.departureTime.isBefore(result.arrivalTime))
    }

    @Test
    fun `should handle null departner in request to entity conversion`() {
        // Given
        val requestWithNullDepartner = sampleRequest.copy(departner = null)

        // When
        val result = EntityMapper.toEntity(requestWithNullDepartner)

        // Then
        assertNotNull(result)
        assertNull(result.departner)
        assertEquals(requestWithNullDepartner.flightId, result.flightId)
    }

    @Test
    fun `should handle null departner in entity to flight DTO conversion`() {
        // Given
        val entityWithNullDepartner = sampleEntity.copy(departner = null)

        // When
        val result = EntityMapper.toFlightDto(entityWithNullDepartner)

        // Then
        assertNotNull(result)
        assertEquals("Unknown Airline", result.airline)
        assertTrue(result.flightNumber.startsWith("UK"))
    }

    @Test
    fun `should handle null seat structure`() {
        // Given
        val requestWithNullSeats = sampleRequest.copy(seatStructure = null)

        // When
        val result = EntityMapper.toEntity(requestWithNullSeats)

        // Then
        assertNotNull(result)
        assertNull(result.seatStructure)
    }

    @Test
    fun `should generate different prices for different stop counts`() {
        // Given
        val entityWithNoStops = sampleEntity.copy(maximumStops = 0)
        val entityWithOneStop = sampleEntity.copy(maximumStops = 1)
        val entityWithTwoStops = sampleEntity.copy(maximumStops = 2)

        // When
        val flightNoStops = EntityMapper.toFlightDto(entityWithNoStops)
        val flightOneStop = EntityMapper.toFlightDto(entityWithOneStop)
        val flightTwoStops = EntityMapper.toFlightDto(entityWithTwoStops)

        // Then
        // Non-stop flights should generally be more expensive (though randomness might affect this)
        assertTrue(flightNoStops.price >= 7500) // Base price * 1.5 = 7500
        assertTrue(flightOneStop.price >= 6000) // Base price * 1.2 = 6000
        assertTrue(flightTwoStops.price >= 5000) // Base price * 1.0 = 5000
    }

    @Test
    fun `should generate flight number with correct format`() {
        // Given
        val entityWithAirIndia = sampleEntity.copy(departner = "Air India")
        val entityWithIndiGo = sampleEntity.copy(departner = "IndiGo")

        // When
        val flightAirIndia = EntityMapper.toFlightDto(entityWithAirIndia)
        val flightIndiGo = EntityMapper.toFlightDto(entityWithIndiGo)

        // Then
        assertTrue(flightAirIndia.flightNumber.startsWith("AI"))
        assertTrue(flightIndiGo.flightNumber.startsWith("IN"))
        assertTrue(flightAirIndia.flightNumber.length == 6) // AI + 4 digits
        assertTrue(flightIndiGo.flightNumber.length == 6) // IN + 4 digits
    }

    @Test
    fun `should generate valid duration format`() {
        // When
        val result = EntityMapper.toFlightDto(sampleEntity)

        // Then
        assertNotNull(result.duration)
        assertTrue(result.duration.matches(Regex("\\d+h \\d+m")))
    }

    @Test
    fun `should set flight date correctly in departure time`() {
        // Given
        val specificDate = LocalDate.of(2024, 12, 25)
        val entityWithSpecificDate = sampleEntity.copy(flightDate = specificDate)

        // When
        val result = EntityMapper.toFlightDto(entityWithSpecificDate)

        // Then
        assertEquals(specificDate, result.departureTime.toLocalDate())
        assertEquals(specificDate, result.arrivalTime.toLocalDate())
    }

    @Test
    fun `should handle edge case with empty airline name`() {
        // Given
        val entityWithEmptyAirline = sampleEntity.copy(departner = "")

        // When
        val result = EntityMapper.toFlightDto(entityWithEmptyAirline)

        // Then
        assertEquals("", result.airline)
        assertTrue(result.flightNumber.startsWith("UK")) // Should default to UK prefix
    }
}
