package com.reservation.service.reservation_system.service

import com.reservation.service.reservation_system.dto.*
import com.reservation.service.reservation_system.repository.TicketRepository
import com.reservation.service.reservation_system.repository.entity.BookingStatus
import com.reservation.service.reservation_system.repository.entity.PaymentStatus
import com.reservation.service.reservation_system.repository.entity.Ticket
import com.reservation.service.reservation_system.repository.entity.cassandra.SeatInfo
import com.reservation.service.reservation_system.repository.entity.cassandra.SeatClass
import com.reservation.service.reservation_system.repository.entity.cassandra.SeatStatus
import com.reservation.service.reservation_system.service.impl.FlightBookingService
import io.mockk.*
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@ExtendWith(MockKExtension::class)
class FlightBookingServiceTest {

    private val ticketRepository = mockk<TicketRepository>()
    private val flightScheduleService = mockk<FlightScheduleService>()
    private val paymentService = mockk<PaymentService>()
    
    private lateinit var flightBookingService: FlightBookingService

    @BeforeEach
    fun setUp() {
        clearAllMocks()
        flightBookingService = FlightBookingService(
            ticketRepository,
            flightScheduleService,
            paymentService
        )
    }

    @Test
    fun `should book segment successfully`() {
        // Given
        val userId = UUID.randomUUID()
        val seatId = UUID.randomUUID()
        val vehicleId = UUID.randomUUID()
        
        val flightDetails = FlightBookingDetails(
            vehicleId = vehicleId,
            flightStartTime = "06:00",
            flightEndTime = "08:00",
            seatId = seatId,
            sourceCode = "DEL",
            destinationCode = "BOM",
            flightTime = "2h",
            discountsApplied = emptyList()
        )
        
        val bookingRequest = BookingRequest(
            userId = userId,
            bookingType = BookingType.FLIGHT,
            bookingDetails = flightDetails
        )

        val availableSeats = listOf(
            SeatInfo(
                seatId = seatId,
                seatNumber = "1A",
                seatClass = SeatClass.BUSINESS,
                amount = BigDecimal("15000.00"),
                seatStatus = SeatStatus.AVAILABLE
            )
        )

        val mockTicket = Ticket(
            ticketId = UUID.randomUUID(),
            userId = userId,
            pnrNumber = "ABC123",
            bookingType = BookingType.FLIGHT,
            bookingStatus = BookingStatus.PENDING,
            paymentStatus = PaymentStatus.PAYMENT_PENDING,
            flightId = vehicleId.toString(),
            seatId = seatId,
            totalAmount = BigDecimal("15000.00")
        )

        val paymentResponse = PaymentResponse(
            transactionId = UUID.randomUUID(),
            status = PaymentStatus.PAYMENT_COMPLETED,
            amount = BigDecimal("15000.00"),
            currency = "INR"
        )

        // Mock behavior
        every { flightScheduleService.getAvailableSeats(any(), any()) } returns availableSeats
        every { flightScheduleService.bookSeatForSegment(any(), any(), any(), any(), any()) } returns true
        every { ticketRepository.save(any()) } returns mockTicket
        every { paymentService.initiatePayment(any()) } returns paymentResponse
        every { flightScheduleService.confirmBlockedSeatByFlightId(any(), any()) } returns mockk()

        // When
        val result = flightBookingService.bookTicket(bookingRequest)

        // Then
        assertNotNull(result)
        assertEquals(userId, result.userId)
        assertEquals(BookingType.FLIGHT, result.bookingType)
        assertEquals(seatId, result.seatId)

        verify { flightScheduleService.bookSeatForSegment(
            flightNumber = vehicleId.toString(),
            date = any(),
            seatId = seatId,
            sourceCode = "DEL",
            destinationCode = "BOM"
        )}
        verify { ticketRepository.save(any()) }
        verify { paymentService.initiatePayment(any()) }
    }

    @Test
    fun `should fail when seat not available for segment`() {
        // Given
        val userId = UUID.randomUUID()
        val seatId = UUID.randomUUID()
        val vehicleId = UUID.randomUUID()
        
        val flightDetails = FlightBookingDetails(
            vehicleId = vehicleId,
            flightStartTime = "06:00",
            flightEndTime = "08:00",
            seatId = seatId,
            sourceCode = "DEL",
            destinationCode = "BOM",
            flightTime = "2h",
            discountsApplied = emptyList()
        )
        
        val bookingRequest = BookingRequest(
            userId = userId,
            bookingType = BookingType.FLIGHT,
            bookingDetails = flightDetails
        )

        val availableSeats = listOf(
            SeatInfo(
                seatId = seatId,
                seatNumber = "1A",
                seatClass = SeatClass.BUSINESS,
                amount = BigDecimal("15000.00"),
                seatStatus = SeatStatus.AVAILABLE
            )
        )

        // Mock behavior - segment booking fails
        every { flightScheduleService.getAvailableSeats(any(), any()) } returns availableSeats
        every { flightScheduleService.bookSeatForSegment(any(), any(), any(), any(), any()) } returns false

        // When & Then
        val exception = assertThrows<IllegalStateException> {
            flightBookingService.bookTicket(bookingRequest)
        }
        
        assertTrue(exception.message!!.contains("Failed to book seat for the requested segment"))
        
        verify { flightScheduleService.bookSeatForSegment(any(), any(), any(), any(), any()) }
        verify(exactly = 0) { ticketRepository.save(any()) }
        verify(exactly = 0) { paymentService.initiatePayment(any()) }
    }

    @Test
    fun `should rollback segment booking on payment failure`() {
        // Given
        val userId = UUID.randomUUID()
        val seatId = UUID.randomUUID()
        val vehicleId = UUID.randomUUID()
        
        val flightDetails = FlightBookingDetails(
            vehicleId = vehicleId,
            flightStartTime = "06:00",
            flightEndTime = "08:00",
            seatId = seatId,
            sourceCode = "DEL",
            destinationCode = "BOM",
            flightTime = "2h",
            discountsApplied = emptyList()
        )
        
        val bookingRequest = BookingRequest(
            userId = userId,
            bookingType = BookingType.FLIGHT,
            bookingDetails = flightDetails
        )

        val availableSeats = listOf(
            SeatInfo(
                seatId = seatId,
                seatNumber = "1A",
                seatClass = SeatClass.BUSINESS,
                amount = BigDecimal("15000.00"),
                seatStatus = SeatStatus.AVAILABLE
            )
        )

        val mockTicket = Ticket(
            ticketId = UUID.randomUUID(),
            userId = userId,
            pnrNumber = "ABC123",
            bookingType = BookingType.FLIGHT,
            bookingStatus = BookingStatus.PENDING,
            paymentStatus = PaymentStatus.PAYMENT_PENDING,
            flightId = vehicleId.toString(),
            seatId = seatId,
            totalAmount = BigDecimal("15000.00")
        )

        // Mock behavior - payment fails
        every { flightScheduleService.getAvailableSeats(any(), any()) } returns availableSeats
        every { flightScheduleService.bookSeatForSegment(any(), any(), any(), any(), any()) } returns true
        every { ticketRepository.save(any()) } returns mockTicket
        every { paymentService.initiatePayment(any()) } throws RuntimeException("Payment failed")
        every { flightScheduleService.getFlightSchedules(any()) } returns emptyList()

        // When & Then
        assertThrows<RuntimeException> {
            flightBookingService.bookTicket(bookingRequest)
        }

        verify { flightScheduleService.bookSeatForSegment(any(), any(), any(), any(), any()) }
        verify { paymentService.initiatePayment(any()) }
        verify { flightScheduleService.getFlightSchedules(any()) } // Rollback attempt
    }

    @Test
    fun `should create ticket with segment information`() {
        // Given
        val userId = UUID.randomUUID()
        val seatId = UUID.randomUUID()
        val vehicleId = UUID.randomUUID()
        
        val flightDetails = FlightBookingDetails(
            vehicleId = vehicleId,
            flightStartTime = "06:00",
            flightEndTime = "08:00",
            seatId = seatId,
            sourceCode = "DEL",
            destinationCode = "BOM",
            flightTime = "2h",
            discountsApplied = emptyList()
        )
        
        val bookingRequest = BookingRequest(
            userId = userId,
            bookingType = BookingType.FLIGHT,
            bookingDetails = flightDetails
        )

        val availableSeats = listOf(
            SeatInfo(
                seatId = seatId,
                seatNumber = "1A",
                seatClass = SeatClass.BUSINESS,
                amount = BigDecimal("15000.00"),
                seatStatus = SeatStatus.AVAILABLE
            )
        )

        val ticketSlot = slot<Ticket>()
        val mockTicket = Ticket(
            ticketId = UUID.randomUUID(),
            userId = userId,
            pnrNumber = "ABC123",
            bookingType = BookingType.FLIGHT,
            bookingStatus = BookingStatus.PENDING,
            paymentStatus = PaymentStatus.PAYMENT_PENDING,
            flightId = vehicleId.toString(),
            seatId = seatId,
            totalAmount = BigDecimal("15000.00"),
            bookingDetails = "segment info"
        )

        val paymentResponse = PaymentResponse(
            transactionId = UUID.randomUUID(),
            status = PaymentStatus.PAYMENT_COMPLETED,
            amount = BigDecimal("15000.00"),
            currency = "INR"
        )

        // Mock behavior
        every { flightScheduleService.getAvailableSeats(any(), any()) } returns availableSeats
        every { flightScheduleService.bookSeatForSegment(any(), any(), any(), any(), any()) } returns true
        every { ticketRepository.save(capture(ticketSlot)) } returns mockTicket
        every { paymentService.initiatePayment(any()) } returns paymentResponse
        every { flightScheduleService.confirmBlockedSeatByFlightId(any(), any()) } returns mockk()

        // When
        flightBookingService.bookTicket(bookingRequest)

        // Then
        val capturedTicket = ticketSlot.captured
        assertNotNull(capturedTicket.bookingDetails)
        assertTrue(capturedTicket.bookingDetails!!.contains("sourceCode"))
        assertTrue(capturedTicket.bookingDetails!!.contains("destinationCode"))
        assertTrue(capturedTicket.bookingDetails!!.contains("DEL"))
        assertTrue(capturedTicket.bookingDetails!!.contains("BOM"))
    }

    @Test
    fun `should handle booking cancellation`() {
        // Given
        val bookingId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        
        val existingTicket = Ticket(
            ticketId = bookingId,
            userId = userId,
            pnrNumber = "ABC123",
            bookingType = BookingType.FLIGHT,
            bookingStatus = BookingStatus.CONFIRMED,
            paymentStatus = PaymentStatus.PAYMENT_COMPLETED,
            flightId = "AI101",
            seatId = UUID.randomUUID(),
            totalAmount = BigDecimal("15000.00")
        )

        val cancelledTicket = existingTicket.copy(bookingStatus = BookingStatus.CANCELLED)

        every { ticketRepository.findByTicketIdAndUserId(bookingId, userId) } returns existingTicket
        every { ticketRepository.save(any()) } returns cancelledTicket

        // When
        val result = flightBookingService.cancelBooking(bookingId, userId)

        // Then
        assertTrue(result)
        verify { ticketRepository.findByTicketIdAndUserId(bookingId, userId) }
        verify { ticketRepository.save(match { it.bookingStatus == BookingStatus.CANCELLED }) }
    }

    @Test
    fun `should get booking details successfully`() {
        // Given
        val bookingId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        
        val existingTicket = Ticket(
            ticketId = bookingId,
            userId = userId,
            pnrNumber = "ABC123",
            bookingType = BookingType.FLIGHT,
            bookingStatus = BookingStatus.CONFIRMED,
            paymentStatus = PaymentStatus.PAYMENT_COMPLETED,
            flightId = "AI101",
            seatId = UUID.randomUUID(),
            totalAmount = BigDecimal("15000.00")
        )

        every { ticketRepository.findByTicketIdAndUserId(bookingId, userId) } returns existingTicket

        // When
        val result = flightBookingService.getBookingDetails(bookingId, userId)

        // Then
        assertNotNull(result)
        assertEquals(bookingId, result!!.ticketId)
        assertEquals(userId, result.userId)
        assertEquals("ABC123", result.pnrNumber)
        
        verify { ticketRepository.findByTicketIdAndUserId(bookingId, userId) }
    }

    @Test
    fun `should return booking type correctly`() {
        // When
        val bookingType = flightBookingService.getBookingType()

        // Then
        assertEquals("FLIGHT", bookingType)
    }
}
