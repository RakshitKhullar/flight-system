package com.reservation.service.reservation_system.service

import com.reservation.service.reservation_system.dto.*
import com.reservation.service.reservation_system.repository.entity.cassandra.*
import com.reservation.service.reservation_system.store.TravelScheduleStore
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FlightScheduleServiceTest {

    private val travelScheduleStore = mockk<TravelScheduleStore>()
    private val flightScheduleService = FlightScheduleService(travelScheduleStore)

    @BeforeEach
    fun setUp() {
        clearAllMocks()
    }

    @Test
    fun `should create multi-stop flight with all segments`() {
        // Given
        val adminId = UUID.randomUUID()
        val request = createMultiStopFlightRequest()
        val mockTravelSchedule = createMockTravelSchedule()

        every { travelScheduleStore.save(any()) } returns mockTravelSchedule

        // When
        val result = flightScheduleService.createFlight(request, adminId)

        // Then
        assertEquals("AI101", result.flightNumber)
        assertEquals("DEL", result.sourceCode)
        assertEquals("BLR", result.destinationCode)
        assertEquals(1, result.numberOfStops)
        assertFalse(result.isDirect)
        assertEquals(1, result.stops.size)
        assertEquals("BOM", result.stops[0].airportCode)
        assertTrue(result.message.contains("segments"))

        verify { travelScheduleStore.save(any()) }
    }

    @Test
    fun `should create correct number of segments for multi-stop flight`() {
        // Given - Flight DEL -> BOM -> BLR should create 3 segments:
        // 1. DEL -> BOM (direct)
        // 2. DEL -> BLR (1 stop: BOM)
        // 3. BOM -> BLR (direct)
        val adminId = UUID.randomUUID()
        val request = createMultiStopFlightRequest()

        val capturedSchedule = slot<TravelSchedule>()
        every { travelScheduleStore.save(capture(capturedSchedule)) } returns createMockTravelSchedule()

        // When
        flightScheduleService.createFlight(request, adminId)

        // Then
        val savedSchedule = capturedSchedule.captured
        assertEquals(3, savedSchedule.schedule.size)

        val segments = savedSchedule.schedule
        val routes = segments.map { "${it.sourceCode}->${it.destinationCode}" }

        assertTrue(routes.contains("DEL->BOM"))
        assertTrue(routes.contains("DEL->BLR"))
        assertTrue(routes.contains("BOM->BLR"))
    }

    @Test
    fun `should book seat for segment successfully`() {
        // Given
        val flightNumber = "AI101"
        val date = LocalDate.now()
        val seatId = UUID.randomUUID()
        val sourceCode = "DEL"
        val destinationCode = "BOM"

        val mockSchedules = createMockSchedulesWithAvailableSeats(seatId)
        every { travelScheduleStore.findByFlightId(flightNumber) } returns mockSchedules
        every { travelScheduleStore.save(any()) } returns mockSchedules.first()

        // When
        val result = flightScheduleService.bookSeatForSegment(
            flightNumber, date, seatId, sourceCode, destinationCode
        )

        // Then
        assertTrue(result)
        verify { travelScheduleStore.save(any()) }
    }

    @Test
    fun `should fail to book seat when not available in overlapping segments`() {
        // Given
        val flightNumber = "AI101"
        val date = LocalDate.now()
        val seatId = UUID.randomUUID()
        val sourceCode = "DEL"
        val destinationCode = "BLR"

        val mockSchedules = createMockSchedulesWithBookedSeats(seatId)
        every { travelScheduleStore.findByFlightId(flightNumber) } returns mockSchedules

        // When
        val result = flightScheduleService.bookSeatForSegment(
            flightNumber, date, seatId, sourceCode, destinationCode
        )

        // Then
        assertFalse(result)
        verify(exactly = 0) { travelScheduleStore.save(any()) }
    }

    @Test
    fun `should check seat availability for segment correctly`() {
        // Given
        val flightNumber = "AI101"
        val date = LocalDate.now()
        val seatId = UUID.randomUUID()
        val sourceCode = "DEL"
        val destinationCode = "BOM"

        val mockSchedules = createMockSchedulesWithAvailableSeats(seatId)
        every { travelScheduleStore.findByFlightId(flightNumber) } returns mockSchedules

        // When
        val isAvailable = flightScheduleService.isSeatAvailableForSegment(
            flightNumber, date, seatId, sourceCode, destinationCode
        )

        // Then
        assertTrue(isAvailable)
    }

    @Test
    fun `should get available seats for specific segment`() {
        // Given
        val flightNumber = "AI101"
        val date = LocalDate.now()
        val sourceCode = "DEL"
        val destinationCode = "BOM"

        val mockSchedules = createMockSchedulesWithAvailableSeats()
        every { travelScheduleStore.findByFlightId(flightNumber) } returns mockSchedules

        // When
        val availableSeats = flightScheduleService.getAvailableSeatsForSegment(
            flightNumber, date, sourceCode, destinationCode
        )

        // Then
        assertTrue(availableSeats.isNotEmpty())
        assertTrue(availableSeats.all { it.seatStatus == SeatStatus.AVAILABLE })
    }

    @Test
    fun `should search flights by segment correctly`() {
        // Given
        val sourceCode = "BOM"
        val destinationCode = "BLR"
        val date = LocalDate.now()

        val mockSchedules = listOf(createMockTravelSchedule())
        every { travelScheduleStore.findAll() } returns mockSchedules

        // When
        val searchResults = flightScheduleService.searchFlights(
            sourceCode, destinationCode, date, directOnly = true
        )

        // Then
        assertTrue(searchResults.isNotEmpty())
        val directFlights = searchResults.filter { it.isDirect }
        assertTrue(directFlights.isNotEmpty())
    }

    @Test
    fun `should handle overlapping segment detection correctly`() {
        // Given - Create segments that should overlap
        val mockSchedules = createMockSchedulesWithOverlappingSegments()
        every { travelScheduleStore.findByFlightId("AI101") } returns mockSchedules

        val seatId = UUID.randomUUID()
        val date = LocalDate.now()

        // When - Try to book overlapping segments
        every { travelScheduleStore.save(any()) } returns mockSchedules.first()
        
        val result1 = flightScheduleService.bookSeatForSegment(
            "AI101", date, seatId, "DEL", "BLR" // Full route
        )
        
        // Update mock to return booked seats
        val bookedSchedules = createMockSchedulesWithBookedSeats(seatId)
        every { travelScheduleStore.findByFlightId("AI101") } returns bookedSchedules
        
        val result2 = flightScheduleService.bookSeatForSegment(
            "AI101", date, seatId, "BOM", "BLR" // Overlapping segment
        )

        // Then
        assertTrue(result1) // First booking should succeed
        assertFalse(result2) // Second booking should fail due to overlap
    }

    // Helper methods
    private fun createMultiStopFlightRequest(): FlightCreationRequest {
        return FlightCreationRequest(
            flightNumber = "AI101",
            date = LocalDate.now(),
            sourceCode = "DEL",
            destinationCode = "BLR",
            travelStartTime = LocalTime.of(6, 0),
            travelEndTime = LocalTime.of(10, 30),
            numberOfStops = 1,
            stops = listOf(
                FlightStopRequest(
                    airportCode = "BOM",
                    airportName = "Mumbai Airport",
                    city = "Mumbai",
                    arrivalTime = LocalTime.of(7, 30),
                    departureTime = LocalTime.of(8, 15),
                    layoverDuration = 45,
                    stopSequence = 1
                )
            ),
            seats = listOf(
                SeatCreationRequest(
                    seatNumber = "1A",
                    seatClass = SeatClass.BUSINESS,
                    amount = BigDecimal("15000.00")
                )
            )
        )
    }

    private fun createMockTravelSchedule(): TravelSchedule {
        val seatInfo = SeatInfo(
            seatNumber = "1A",
            seatClass = SeatClass.BUSINESS,
            amount = BigDecimal("15000.00"),
            seatStatus = SeatStatus.AVAILABLE
        )

        val stop = FlightStop(
            airportCode = "BOM",
            airportName = "Mumbai Airport",
            city = "Mumbai",
            arrivalTime = LocalTime.of(7, 30),
            departureTime = LocalTime.of(8, 15),
            layoverDuration = 45,
            stopSequence = 1
        )

        val scheduleItems = listOf(
            // DEL -> BOM
            ScheduleItem(
                date = LocalDate.now(),
                flightNumber = "AI101",
                sourceCode = "DEL",
                destinationCode = "BOM",
                travelStartTime = LocalTime.of(6, 0),
                travelEndTime = LocalTime.of(7, 30),
                seats = listOf(seatInfo),
                totalSeats = 1,
                availableSeats = 1,
                numberOfStops = 0,
                stops = emptyList(),
                isDirect = true
            ),
            // DEL -> BLR (full route)
            ScheduleItem(
                date = LocalDate.now(),
                flightNumber = "AI101",
                sourceCode = "DEL",
                destinationCode = "BLR",
                travelStartTime = LocalTime.of(6, 0),
                travelEndTime = LocalTime.of(10, 30),
                seats = listOf(seatInfo),
                totalSeats = 1,
                availableSeats = 1,
                numberOfStops = 1,
                stops = listOf(stop),
                isDirect = false
            ),
            // BOM -> BLR
            ScheduleItem(
                date = LocalDate.now(),
                flightNumber = "AI101",
                sourceCode = "BOM",
                destinationCode = "BLR",
                travelStartTime = LocalTime.of(8, 15),
                travelEndTime = LocalTime.of(10, 30),
                seats = listOf(seatInfo),
                totalSeats = 1,
                availableSeats = 1,
                numberOfStops = 0,
                stops = emptyList(),
                isDirect = true
            )
        )

        return TravelSchedule(
            vehicleId = "AI101",
            schedule = scheduleItems
        )
    }

    private fun createMockSchedulesWithAvailableSeats(seatId: UUID = UUID.randomUUID()): List<TravelSchedule> {
        val seatInfo = SeatInfo(
            seatId = seatId,
            seatNumber = "1A",
            seatClass = SeatClass.BUSINESS,
            amount = BigDecimal("15000.00"),
            seatStatus = SeatStatus.AVAILABLE
        )

        val scheduleItem = ScheduleItem(
            date = LocalDate.now(),
            flightNumber = "AI101",
            sourceCode = "DEL",
            destinationCode = "BOM",
            travelStartTime = LocalTime.of(6, 0),
            travelEndTime = LocalTime.of(7, 30),
            seats = listOf(seatInfo),
            totalSeats = 1,
            availableSeats = 1,
            numberOfStops = 0,
            stops = emptyList(),
            isDirect = true
        )

        return listOf(
            TravelSchedule(
                vehicleId = "AI101",
                schedule = listOf(scheduleItem)
            )
        )
    }

    private fun createMockSchedulesWithBookedSeats(seatId: UUID): List<TravelSchedule> {
        val seatInfo = SeatInfo(
            seatId = seatId,
            seatNumber = "1A",
            seatClass = SeatClass.BUSINESS,
            amount = BigDecimal("15000.00"),
            seatStatus = SeatStatus.BOOKED
        )

        val scheduleItem = ScheduleItem(
            date = LocalDate.now(),
            flightNumber = "AI101",
            sourceCode = "DEL",
            destinationCode = "BOM",
            travelStartTime = LocalTime.of(6, 0),
            travelEndTime = LocalTime.of(7, 30),
            seats = listOf(seatInfo),
            totalSeats = 1,
            availableSeats = 0,
            numberOfStops = 0,
            stops = emptyList(),
            isDirect = true
        )

        return listOf(
            TravelSchedule(
                vehicleId = "AI101",
                schedule = listOf(scheduleItem)
            )
        )
    }

    private fun createMockSchedulesWithOverlappingSegments(): List<TravelSchedule> {
        val seatInfo = SeatInfo(
            seatNumber = "1A",
            seatClass = SeatClass.BUSINESS,
            amount = BigDecimal("15000.00"),
            seatStatus = SeatStatus.AVAILABLE
        )

        val stop = FlightStop(
            airportCode = "BOM",
            airportName = "Mumbai Airport",
            city = "Mumbai",
            arrivalTime = LocalTime.of(7, 30),
            departureTime = LocalTime.of(8, 15),
            layoverDuration = 45,
            stopSequence = 1
        )

        val scheduleItems = listOf(
            // DEL -> BOM
            ScheduleItem(
                date = LocalDate.now(),
                flightNumber = "AI101",
                sourceCode = "DEL",
                destinationCode = "BOM",
                travelStartTime = LocalTime.of(6, 0),
                travelEndTime = LocalTime.of(7, 30),
                seats = listOf(seatInfo.copy()),
                totalSeats = 1,
                availableSeats = 1,
                numberOfStops = 0,
                stops = emptyList(),
                isDirect = true
            ),
            // DEL -> BLR (overlaps with DEL->BOM and BOM->BLR)
            ScheduleItem(
                date = LocalDate.now(),
                flightNumber = "AI101",
                sourceCode = "DEL",
                destinationCode = "BLR",
                travelStartTime = LocalTime.of(6, 0),
                travelEndTime = LocalTime.of(10, 30),
                seats = listOf(seatInfo.copy()),
                totalSeats = 1,
                availableSeats = 1,
                numberOfStops = 1,
                stops = listOf(stop),
                isDirect = false
            ),
            // BOM -> BLR
            ScheduleItem(
                date = LocalDate.now(),
                flightNumber = "AI101",
                sourceCode = "BOM",
                destinationCode = "BLR",
                travelStartTime = LocalTime.of(8, 15),
                travelEndTime = LocalTime.of(10, 30),
                seats = listOf(seatInfo.copy()),
                totalSeats = 1,
                availableSeats = 1,
                numberOfStops = 0,
                stops = emptyList(),
                isDirect = true
            )
        )

        return listOf(
            TravelSchedule(
                vehicleId = "AI101",
                schedule = scheduleItems
            )
        )
    }
}
