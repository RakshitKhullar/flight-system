package com.reservation.service.reservation_system.handler

import com.reservation.service.reservation_system.dto.*
import com.reservation.service.reservation_system.exception.SeatBookingInProgressException
import com.reservation.service.reservation_system.factory.BookingServiceFactory
import com.reservation.service.reservation_system.repository.entity.BookingStatus
import com.reservation.service.reservation_system.repository.entity.PaymentStatus
import com.reservation.service.reservation_system.repository.entity.Ticket
import com.reservation.service.reservation_system.service.BookingService
import com.reservation.service.reservation_system.service.SeatBookingCacheService
import com.reservation.service.reservation_system.service.SeatKeyService
import io.mockk.*
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@ExtendWith(MockKExtension::class)
class BookingHandlerTest {

    private val bookingServiceFactory = mockk<BookingServiceFactory>()
    private val seatBookingCacheService = mockk<SeatBookingCacheService>()
    private val seatKeyService = mockk<SeatKeyService>()
    private val bookingService = mockk<BookingService>()

    private lateinit var bookingHandler: BookingHandler

    private val userId = UUID.randomUUID()
    private val ticketId = UUID.randomUUID()
    private val seatId = UUID.randomUUID()
    private val vehicleId = UUID.randomUUID()
    private val seatKey = "FL123:$seatId:10:30"

    @BeforeEach
    fun setUp() {
        bookingHandler = BookingHandler(
            bookingServiceFactory,
            seatBookingCacheService,
            seatKeyService
        )
    }

    @Test
    fun `should process flight booking successfully when seat is not blocked`() = runTest {
        // Given
        val bookingRequest = createFlightBookingRequest()
        val expectedTicket = createTicket()

        every { seatKeyService.generateSeatKey(any<BookingDetails>()) } returns seatKey
        every { seatBookingCacheService.isSeatBookingInProgress(seatKey) } returns false
        every { seatBookingCacheService.blockSeatForBooking(seatKey) } returns true
        every { seatBookingCacheService.releaseSeatBooking(seatKey) } just Runs
        every { bookingServiceFactory.getService("FLIGHT") } returns bookingService
        every { bookingService.bookTicket(any()) } returns expectedTicket

        // When
        val result = bookingHandler.processBooking(bookingRequest)

        // Then
        assertNotNull(result)
        assertEquals(expectedTicket.ticketId, result.ticketId)
        assertEquals(expectedTicket.pnrNumber, result.pnrNumber)

        verify { seatKeyService.generateSeatKey(any<BookingDetails>()) }
        verify { seatBookingCacheService.isSeatBookingInProgress(seatKey) }
        verify { seatBookingCacheService.blockSeatForBooking(seatKey) }
        verify { bookingService.bookTicket(any()) }
        verify { seatBookingCacheService.releaseSeatBooking(seatKey) }
    }

    @Test
    fun `should throw exception when seat is already blocked initially`() = runTest {
        // Given
        val bookingRequest = createFlightBookingRequest()

        every { seatKeyService.generateSeatKey(any<BookingDetails>()) } returns seatKey
        every { seatBookingCacheService.isSeatBookingInProgress(seatKey) } returns true

        // When & Then
        val exception = assertThrows<SeatBookingInProgressException> {
            bookingHandler.processBooking(bookingRequest)
        }

        assertEquals("Seat booking in progress, kindly select another seat", exception.message)

        verify { seatKeyService.generateSeatKey(any<BookingDetails>()) }
        verify { seatBookingCacheService.isSeatBookingInProgress(seatKey) }
        verify(exactly = 0) { seatBookingCacheService.blockSeatForBooking(any()) }
    }

    @Test
    fun `should throw exception when seat becomes blocked after delay`() = runTest {
        // Given
        val bookingRequest = createFlightBookingRequest()

        every { seatKeyService.generateSeatKey(any<BookingDetails>()) } returns seatKey
        every { seatBookingCacheService.isSeatBookingInProgress(seatKey) } returnsMany listOf(false, true)

        // When & Then
        val exception = assertThrows<SeatBookingInProgressException> {
            bookingHandler.processBooking(bookingRequest)
        }

        assertEquals("Seat booking in progress, kindly select another seat", exception.message)

        verify(exactly = 2) { seatBookingCacheService.isSeatBookingInProgress(seatKey) }
        verify(exactly = 0) { seatBookingCacheService.blockSeatForBooking(any()) }
    }

    @Test
    fun `should throw exception when atomic blocking fails`() = runTest {
        // Given
        val bookingRequest = createFlightBookingRequest()

        every { seatKeyService.generateSeatKey(any<BookingDetails>()) } returns seatKey
        every { seatBookingCacheService.isSeatBookingInProgress(seatKey) } returns false
        every { seatBookingCacheService.blockSeatForBooking(seatKey) } returns false

        // When & Then
        val exception = assertThrows<SeatBookingInProgressException> {
            bookingHandler.processBooking(bookingRequest)
        }

        assertEquals("Seat booking in progress, kindly select another seat", exception.message)

        verify { seatBookingCacheService.blockSeatForBooking(seatKey) }
        verify(exactly = 0) { bookingServiceFactory.getService(any()) }
    }

    @Test
    fun `should release seat when booking fails`() = runTest {
        // Given
        val bookingRequest = createFlightBookingRequest()
        val bookingException = RuntimeException("Booking failed")

        every { seatKeyService.generateSeatKey(any<BookingDetails>()) } returns seatKey
        every { seatBookingCacheService.isSeatBookingInProgress(seatKey) } returns false
        every { seatBookingCacheService.blockSeatForBooking(seatKey) } returns true
        every { seatBookingCacheService.releaseSeatBooking(seatKey) } just Runs
        every { bookingServiceFactory.getService("FLIGHT") } returns bookingService
        every { bookingService.bookTicket(any()) } throws bookingException

        // When & Then
        val exception = assertThrows<RuntimeException> {
            bookingHandler.processBooking(bookingRequest)
        }

        assertEquals("Booking failed", exception.message)

        verify { seatBookingCacheService.blockSeatForBooking(seatKey) }
        verify { bookingService.bookTicket(any()) }
        verify { seatBookingCacheService.releaseSeatBooking(seatKey) }
    }

    @Test
    fun `should process non-flight booking without cache logic`() = runTest {
        // Given
        val bookingRequest = createHotelBookingRequest()
        val expectedTicket = createTicket()

        every { bookingServiceFactory.getService("HOTEL") } returns bookingService
        every { bookingService.bookTicket(any()) } returns expectedTicket

        // When
        val result = bookingHandler.processBooking(bookingRequest)

        // Then
        assertNotNull(result)
        assertEquals(expectedTicket.ticketId, result.ticketId)

        verify { bookingServiceFactory.getService("HOTEL") }
        verify { bookingService.bookTicket(any()) }
        verify(exactly = 0) { seatKeyService.generateSeatKey(any<BookingDetails>()) }
        verify(exactly = 0) { seatBookingCacheService.isSeatBookingInProgress(any()) }
    }

    @Test
    fun `should process cancellation successfully`() {
        // Given
        val bookingId = UUID.randomUUID()
        val bookingType = "FLIGHT"

        every { bookingServiceFactory.getService(bookingType) } returns bookingService
        every { bookingService.cancelBooking(bookingId, userId) } returns true

        // When
        val result = bookingHandler.processCancellation(bookingId, userId, bookingType)

        // Then
        assertEquals(true, result)

        verify { bookingServiceFactory.getService(bookingType) }
        verify { bookingService.cancelBooking(bookingId, userId) }
    }

    @Test
    fun `should get booking details successfully`() {
        // Given
        val bookingId = UUID.randomUUID()
        val bookingType = "FLIGHT"
        val expectedTicket = createTicket()

        every { bookingServiceFactory.getService(bookingType) } returns bookingService
        every { bookingService.getBookingDetails(bookingId, userId) } returns expectedTicket

        // When
        val result = bookingHandler.getBookingDetails(bookingId, userId, bookingType)

        // Then
        assertNotNull(result)
        assertEquals(expectedTicket.ticketId, result.ticketId)

        verify { bookingServiceFactory.getService(bookingType) }
        verify { bookingService.getBookingDetails(bookingId, userId) }
    }

    private fun createFlightBookingRequest(): BookingRequest {
        val flightDetails = FlightBookingDetails(
            vehicleId = vehicleId,
            flightStartTime = "10:00",
            flightEndTime = "12:00",
            seatId = seatId,
            sourceCode = "DEL",
            destinationCode = "BOM",
            flightTime = "10:30",
            discountsApplied = emptyList()
        )

        return BookingRequest(
            userId = userId,
            bookingType = BookingType.FLIGHT,
            bookingDetails = flightDetails
        )
    }

    private fun createHotelBookingRequest(): BookingRequest {
        // Mock hotel booking details
        val hotelDetails = object : BookingDetails {
            override fun getBookingType(): String = "HOTEL"
        }

        return BookingRequest(
            userId = userId,
            bookingType = BookingType.HOTEL,
            bookingDetails = hotelDetails
        )
    }

    private fun createTicket(): Ticket {
        return Ticket(
            ticketId = ticketId,
            userId = userId,
            pnrNumber = "PNR123456",
            bookingType = BookingType.FLIGHT,
            bookingStatus = BookingStatus.CONFIRMED,
            paymentStatus = PaymentStatus.PAYMENT_PENDING,
            flightId = vehicleId.toString(),
            seatId = seatId,
            totalAmount = BigDecimal("5000.00"),
            bookingDetails = null,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
    }
}
