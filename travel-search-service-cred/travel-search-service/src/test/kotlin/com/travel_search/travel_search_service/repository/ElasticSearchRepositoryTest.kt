package com.travel_search.travel_search_service.repository

import com.fasterxml.jackson.databind.ObjectMapper
import com.travel_search.travel_search_service.repository.entity.ElasticSearch
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDate
import java.time.ZonedDateTime

@DataJpaTest
@ActiveProfiles("test")
@ExtendWith(MockitoExtension::class)
class ElasticSearchRepositoryTest {

    @Autowired
    private lateinit var elasticSearchRepository: ElasticSearchRepository

    private val objectMapper = ObjectMapper()
    private lateinit var sampleFlight: ElasticSearch

    @BeforeEach
    fun setUp() {
        elasticSearchRepository.deleteAll()
        
        val seatStructure = objectMapper.readTree("""{"economy": 150, "business": 20, "first": 10}""")
        
        sampleFlight = ElasticSearch(
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
    fun `should save and find flight by ID`() {
        // Given
        val savedFlight = elasticSearchRepository.save(sampleFlight)

        // When
        val foundFlight = elasticSearchRepository.findById(savedFlight.id)

        // Then
        assertTrue(foundFlight.isPresent)
        assertEquals(sampleFlight.flightId, foundFlight.get().flightId)
        assertEquals(sampleFlight.source, foundFlight.get().source)
        assertEquals(sampleFlight.destination, foundFlight.get().destination)
    }

    @Test
    fun `should find flight by flight ID`() {
        // Given
        elasticSearchRepository.save(sampleFlight)

        // When
        val foundFlight = elasticSearchRepository.findByFlightId("AI101")

        // Then
        assertNotNull(foundFlight)
        assertEquals("AI101", foundFlight?.flightId)
        assertEquals("Delhi", foundFlight?.source)
        assertEquals("Mumbai", foundFlight?.destination)
    }

    @Test
    fun `should return null when flight ID not found`() {
        // When
        val foundFlight = elasticSearchRepository.findByFlightId("NONEXISTENT")

        // Then
        assertNull(foundFlight)
    }

    @Test
    fun `should find all flights by flight ID`() {
        // Given
        val flight1 = sampleFlight.copy(destination = "Mumbai")
        val flight2 = sampleFlight.copy(destination = "Bangalore")
        
        elasticSearchRepository.save(flight1)
        elasticSearchRepository.save(flight2)

        // When
        val foundFlights = elasticSearchRepository.findAllByFlightId("AI101")

        // Then
        assertEquals(2, foundFlights.size)
        assertTrue(foundFlights.all { it.flightId == "AI101" })
        assertTrue(foundFlights.any { it.destination == "Mumbai" })
        assertTrue(foundFlights.any { it.destination == "Bangalore" })
    }

    @Test
    fun `should find flights by source, destination and flight date`() {
        // Given
        val flightDate = LocalDate.now().plusDays(1)
        val flight1 = sampleFlight.copy(flightDate = flightDate)
        val flight2 = sampleFlight.copy(flightId = "6E202", flightDate = flightDate)
        val flight3 = sampleFlight.copy(destination = "Bangalore", flightDate = flightDate)
        
        elasticSearchRepository.save(flight1)
        elasticSearchRepository.save(flight2)
        elasticSearchRepository.save(flight3)

        // When
        val foundFlights = elasticSearchRepository.findBySourceAndDestinationAndFlightDate(
            "Delhi", "Mumbai", flightDate
        )

        // Then
        assertEquals(2, foundFlights.size)
        assertTrue(foundFlights.all { it.source == "Delhi" && it.destination == "Mumbai" })
    }

    @Test
    fun `should find flights by criteria with maximum stops filter`() {
        // Given
        val flightDate = LocalDate.now().plusDays(1)
        val flight1 = sampleFlight.copy(maximumStops = 0, flightDate = flightDate)
        val flight2 = sampleFlight.copy(flightId = "6E202", maximumStops = 1, flightDate = flightDate)
        val flight3 = sampleFlight.copy(flightId = "SG303", maximumStops = 2, flightDate = flightDate)
        
        elasticSearchRepository.save(flight1)
        elasticSearchRepository.save(flight2)
        elasticSearchRepository.save(flight3)

        // When
        val foundFlights = elasticSearchRepository.findBySourceAndDestinationAndFlightDateAndMaximumStopsLessThanEqual(
            "Delhi", "Mumbai", flightDate, 1
        )

        // Then
        assertEquals(2, foundFlights.size)
        assertTrue(foundFlights.all { it.maximumStops <= 1 })
    }

    @Test
    fun `should find flights by departner`() {
        // Given
        val flight1 = sampleFlight.copy(departner = "Air India")
        val flight2 = sampleFlight.copy(flightId = "6E202", departner = "IndiGo")
        val flight3 = sampleFlight.copy(flightId = "SG303", departner = "Air India")
        
        elasticSearchRepository.save(flight1)
        elasticSearchRepository.save(flight2)
        elasticSearchRepository.save(flight3)

        // When
        val foundFlights = elasticSearchRepository.findByDepartner("Air India")

        // Then
        assertEquals(2, foundFlights.size)
        assertTrue(foundFlights.all { it.departner == "Air India" })
    }

    @Test
    fun `should find flights by search criteria with all parameters`() {
        // Given
        val flightDate = LocalDate.now().plusDays(1)
        val flight1 = sampleFlight.copy(
            flightId = "AI101",
            source = "Delhi",
            destination = "Mumbai",
            flightDate = flightDate,
            maximumStops = 0,
            departner = "Air India"
        )
        val flight2 = sampleFlight.copy(
            flightId = "6E202",
            source = "Delhi",
            destination = "Mumbai",
            flightDate = flightDate,
            maximumStops = 1,
            departner = "IndiGo"
        )
        
        elasticSearchRepository.save(flight1)
        elasticSearchRepository.save(flight2)

        // When
        val foundFlights = elasticSearchRepository.findFlightsBySearchCriteria(
            source = "Delhi",
            destination = "Mumbai",
            flightDate = flightDate,
            departner = "Air India",
            maxStops = 1
        )

        // Then
        assertEquals(1, foundFlights.size)
        assertEquals("AI101", foundFlights[0].flightId)
        assertEquals("Air India", foundFlights[0].departner)
    }

    @Test
    fun `should find flights by search criteria with null departner`() {
        // Given
        val flightDate = LocalDate.now().plusDays(1)
        val flight1 = sampleFlight.copy(flightDate = flightDate, departner = "Air India")
        val flight2 = sampleFlight.copy(flightId = "6E202", flightDate = flightDate, departner = "IndiGo")
        
        elasticSearchRepository.save(flight1)
        elasticSearchRepository.save(flight2)

        // When
        val foundFlights = elasticSearchRepository.findFlightsBySearchCriteria(
            source = "Delhi",
            destination = "Mumbai",
            flightDate = flightDate,
            departner = null,
            maxStops = 2
        )

        // Then
        assertEquals(2, foundFlights.size)
    }

    @Test
    fun `should find flight by exact criteria match`() {
        // Given
        val flightDate = LocalDate.now().plusDays(1)
        val flight = sampleFlight.copy(
            flightId = "AI101",
            source = "Delhi",
            destination = "Mumbai",
            flightDate = flightDate,
            maximumStops = 0,
            departner = "Air India"
        )
        elasticSearchRepository.save(flight)

        // When
        val foundFlight = elasticSearchRepository.findByFlightIdAndCriteria(
            flightId = "AI101",
            source = "Delhi",
            destination = "Mumbai",
            flightDate = flightDate,
            maximumStops = 0,
            departner = "Air India"
        )

        // Then
        assertNotNull(foundFlight)
        assertEquals("AI101", foundFlight?.flightId)
    }

    @Test
    fun `should return null when no exact criteria match found`() {
        // Given
        val flightDate = LocalDate.now().plusDays(1)
        val flight = sampleFlight.copy(
            flightId = "AI101",
            source = "Delhi",
            destination = "Mumbai",
            flightDate = flightDate,
            maximumStops = 0,
            departner = "Air India"
        )
        elasticSearchRepository.save(flight)

        // When
        val foundFlight = elasticSearchRepository.findByFlightIdAndCriteria(
            flightId = "AI101",
            source = "Delhi",
            destination = "Bangalore", // Different destination
            flightDate = flightDate,
            maximumStops = 0,
            departner = "Air India"
        )

        // Then
        assertNull(foundFlight)
    }

    @Test
    fun `should find all distinct sources`() {
        // Given
        val flight1 = sampleFlight.copy(source = "Delhi")
        val flight2 = sampleFlight.copy(flightId = "6E202", source = "Mumbai")
        val flight3 = sampleFlight.copy(flightId = "SG303", source = "Delhi") // Duplicate
        
        elasticSearchRepository.save(flight1)
        elasticSearchRepository.save(flight2)
        elasticSearchRepository.save(flight3)

        // When
        val sources = elasticSearchRepository.findAllSources()

        // Then
        assertEquals(2, sources.size)
        assertTrue(sources.contains("Delhi"))
        assertTrue(sources.contains("Mumbai"))
    }

    @Test
    fun `should find all distinct destinations`() {
        // Given
        val flight1 = sampleFlight.copy(destination = "Mumbai")
        val flight2 = sampleFlight.copy(flightId = "6E202", destination = "Bangalore")
        val flight3 = sampleFlight.copy(flightId = "SG303", destination = "Mumbai") // Duplicate
        
        elasticSearchRepository.save(flight1)
        elasticSearchRepository.save(flight2)
        elasticSearchRepository.save(flight3)

        // When
        val destinations = elasticSearchRepository.findAllDestinations()

        // Then
        assertEquals(2, destinations.size)
        assertTrue(destinations.contains("Mumbai"))
        assertTrue(destinations.contains("Bangalore"))
    }

    @Test
    fun `should find all distinct departners excluding nulls`() {
        // Given
        val flight1 = sampleFlight.copy(departner = "Air India")
        val flight2 = sampleFlight.copy(flightId = "6E202", departner = "IndiGo")
        val flight3 = sampleFlight.copy(flightId = "SG303", departner = null)
        val flight4 = sampleFlight.copy(flightId = "UK404", departner = "Air India") // Duplicate
        
        elasticSearchRepository.save(flight1)
        elasticSearchRepository.save(flight2)
        elasticSearchRepository.save(flight3)
        elasticSearchRepository.save(flight4)

        // When
        val departners = elasticSearchRepository.findAllDepartners()

        // Then
        assertEquals(2, departners.size)
        assertTrue(departners.contains("Air India"))
        assertTrue(departners.contains("IndiGo"))
        assertFalse(departners.contains(null))
    }

    @Test
    fun `should handle JSON seat structure correctly`() {
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
                }
            }
        """)
        
        val flightWithComplexSeats = sampleFlight.copy(seatStructure = complexSeatStructure)
        
        // When
        val savedFlight = elasticSearchRepository.save(flightWithComplexSeats)
        val foundFlight = elasticSearchRepository.findById(savedFlight.id)

        // Then
        assertTrue(foundFlight.isPresent)
        assertNotNull(foundFlight.get().seatStructure)
        assertEquals(150, foundFlight.get().seatStructure?.get("economy")?.get("total")?.asInt())
        assertEquals(20, foundFlight.get().seatStructure?.get("business")?.get("total")?.asInt())
    }

    @Test
    fun `should handle null seat structure`() {
        // Given
        val flightWithNullSeats = sampleFlight.copy(seatStructure = null)
        
        // When
        val savedFlight = elasticSearchRepository.save(flightWithNullSeats)
        val foundFlight = elasticSearchRepository.findById(savedFlight.id)

        // Then
        assertTrue(foundFlight.isPresent)
        assertNull(foundFlight.get().seatStructure)
    }
}
