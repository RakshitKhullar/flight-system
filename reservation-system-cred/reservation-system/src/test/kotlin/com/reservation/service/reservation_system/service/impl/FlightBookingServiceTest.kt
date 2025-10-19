package com.reservation.service.reservation_system.service.impl

import com.reservation.service.reservation_system.dto.*
import com.reservation.service.reservation_system.repository.TicketRepository
import com.reservation.service.reservation_system.repository.entity.BookingStatus
import com.reservation.service.reservation_system.repository.entity.PaymentStatus
import com.reservation.service.reservation_system.repository.entity.Ticket
import com.reservation.service.reservation_system.repository.entity.cassandra.*
import com.reservation.service.reservation_system.service.FlightScheduleService
import com.reservation.service.reservation_system.service.PaymentService
import io.mockk.*
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@ExtendWith(MockKExtension::class)
class FlightBookingServiceTest {

    private val ticketRepository = mockk<TicketRepository>()
    private val flightScheduleService = mockk<FlightScheduleService>()
    private val paymentService = mockk<PaymentService>()

    private lateinit var flightBookingService: FlightBookingService

    private val userId = UUID.randomUUID()
    private val ticketId = UUID.randomUUID()
    private val seatId = UUID.randomUUID()
    private val vehicleId = UUID.randomUUID()
    private val scheduleId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        flightBookingService = FlightBookingService(
            ticketRepository,
            flightScheduleService,
            paymentService
        )
    }

    @Test
    fun `should book flight ticket successfully`() {
        // Given
        val bookingRequest = createBookingRequest()
        val availableSeats = createAvailableSeats()
        val travelSchedule = createTravelSchedule()
        val paymentResponse = createPaymentResponse()
        val savedTicket = createTicket()

        every { flightScheduleService.getAvailableSeats(any(), any()) } returns availableSeats
        every { flightScheduleService.updateSeatStatusByFlightId(any(), any(), SeatStatus.BLOCKED) } returns travelSchedule
        every { paymentService.initiatePayment(any()) } returns paymentResponse
        every { ticketRepository.save(any()) } returns savedTicket

        // When
        val result = flightBookingService.bookTicket(bookingRequest)

        // Then
        assertNotNull(result)
        assertEquals(savedTicket.ticketId, result.ticketId)
        assertEquals(savedTicket.pnrNumber, result.pnrNumber)
        assertEquals(BookingStatus.CONFIRMED, result.bookingStatus)

        verify { flightScheduleService.getAvailableSeats(vehicleId.toString(), any()) }
        verify { flightScheduleService.updateSeatStatusByFlightId(vehicleId.toString(), seatId, SeatStatus.BLOCKED) }
        verify { paymentService.initiatePayment(any()) }
        verify { ticketRepository.save(any()) }
    }

    @Test
    fun `should throw exception when no available seats`() {
        // Given
        val bookingRequest = createBookingRequest()

        every { flightScheduleService.getAvailableSeats(any(), any()) } returns emptyList()

        // When & Then
        val exception = assertThrows<IllegalStateException> {
            flightBookingService.bookTicket(bookingRequest)
        }

        assertEquals("No available seats found for the requested flight", exception.message)

        verify { flightScheduleService.getAvailableSeats(vehicleId.toString(), any()) }
        verify(exactly = 0) { flightScheduleService.updateSeatStatusByFlightId(any(), any(), any()) }
        verify(exactly = 0) { paymentService.initiatePayment(any()) }
    }

    @Test
    fun `should throw exception when requested seat is not available`() {
        // Given
        val bookingRequest = createBookingRequest()
        val availableSeats = createAvailableSeatsWithDifferentSeatId()

        every { flightScheduleService.getAvailableSeats(any(), any()) } returns availableSeats

        // When & Then
        val exception = assertThrows<IllegalStateException> {
            flightBookingService.bookTicket(bookingRequest)
        }

        assertEquals("Requested seat is not available", exception.message)

        verify { flightScheduleService.getAvailableSeats(vehicleId.toString(), any()) }
        verify(exactly = 0) { flightScheduleService.updateSeatStatusByFlightId(any(), any(), any()) }
    }

    @Test
    fun `should rollback seat blocking when payment fails`() {
        // Given
        val bookingRequest = createBookingRequest()
        val availableSeats = createAvailableSeats()
        val travelSchedule = createTravelSchedule()
        val paymentException = RuntimeException("Payment failed")

        every { flightScheduleService.getAvailableSeats(any(), any()) } returns availableSeats
        every { flightScheduleService.updateSeatStatusByFlightId(any(), any(), SeatStatus.BLOCKED) } returns travelSchedule
        every { flightScheduleService.updateSeatStatusByFlightId(any(), any(), SeatStatus.AVAILABLE) } returns travelSchedule
        every { paymentService.initiatePayment(any()) } throws paymentException

        // When & Then
        val exception = assertThrows<RuntimeException> {
            flightBookingService.bookTicket(bookingRequest)
        }

        assertEquals("Payment failed", exception.message)

        verify { flightScheduleService.updateSeatStatusByFlightId(vehicleId.toString(), seatId, SeatStatus.BLOCKED) }
        verify { flightScheduleService.updateSeatStatusByFlightId(vehicleId.toString(), seatId, SeatStatus.AVAILABLE) }
        verify(exactly = 0) { ticketRepository.save(any()) }
    }

    @Test
    fun `should cancel booking successfully`() {
        // Given
        val bookingId = UUID.randomUUID()
        val existingTicket = createTicket()
        val updatedTicket = existingTicket.copy(bookingStatus = BookingStatus.CANCELLED)

        every { ticketRepository.findByTicketIdAndUserId(bookingId, userId) } returns existingTicket
        every { ticketRepository.save(any()) } returns updatedTicket

        // When
        val result = flightBookingService.cancelBooking(bookingId, userId)

        // Then
        assertEquals(true, result)

        verify { ticketRepository.findByTicketIdAndUserId(bookingId, userId) }
        verify { ticketRepository.save(match { it.bookingStatus == BookingStatus.CANCELLED }) }
    }

    @Test
    fun `should return false when booking not found for cancellation`() {
        // Given
        val bookingId = UUID.randomUUID()

        every { ticketRepository.findByTicketIdAndUserId(bookingId, userId) } returns null

        // When
        val result = flightBookingService.cancelBooking(bookingId, userId)

        // Then
        assertEquals(false, result)

        verify { ticketRepository.findByTicketIdAndUserId(bookingId, userId) }
        verify(exactly = 0) { ticketRepository.save(any()) }
    }

    @Test
    fun `should get booking details successfully`() {
        // Given
        val bookingId = UUID.randomUUID()
        val expectedTicket = createTicket()

        every { ticketRepository.findByTicketIdAndUserId(bookingId, userId) } returns expectedTicket

        // When
        val result = flightBookingService.getBookingDetails(bookingId, userId)

        // Then
        assertNotNull(result)
        assertEquals(expectedTicket.ticketId, result.ticketId)
        assertEquals(expectedTicket.pnrNumber, result.pnrNumber)

        verify { ticketRepository.findByTicketIdAndUserId(bookingId, userId) }
    }

    @Test
    fun `should return null when booking details not found`() {
        // Given
        val bookingId = UUID.randomUUID()

        every { ticketRepository.findByTicketIdAndUserId(bookingId, userId) } returns null

        // When
        val result = flightBookingService.getBookingDetails(bookingId, userId)

        // Then
        assertEquals(null, result)

        verify { ticketRepository.findByTicketIdAndUserId(bookingId, userId) }
    }

    private fun createBookingRequest(): BookingRequest {
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

    private fun createAvailableSeats(): List<SeatInfo> {
        return listOf(
            SeatInfo(
                seatId = seatId,
                seatNumber = "A1",
                seatClass = SeatClass.ECONOMY,
                amount = BigDecimal("5000.00"),
                seatStatus = SeatStatus.AVAILABLE
            )
        )
    }

    private fun createAvailableSeatsWithDifferentSeatId(): List<SeatInfo> {
        return listOf(
            SeatInfo(
                seatId = UUID.randomUUID(),
                seatNumber = "A2",
                seatClass = SeatClass.ECONOMY,
                amount = BigDecimal("5000.00"),
                seatStatus = SeatStatus.AVAILABLE
            )
        )
    }

    private fun createTravelSchedule(): TravelSchedule {
        return TravelSchedule(
            id = scheduleId,
            vehicleId = vehicleId.toString(),
            schedule = listOf(
                ScheduleItem(
                    scheduleId = UUID.randomUUID(),
                    date = LocalDate.now(),
                    flightNumber = "FL123",
                    sourceCode = "DEL",
                    destinationCode = "BOM",
                    travelStartTime = LocalTime.of(10, 0),
                    travelEndTime = LocalTime.of(12, 0),
                    seats = createAvailableSeats(),
                    totalSeats = 180,
                    availableSeats = 179
                )
            )
        )
    }

    private fun createPaymentResponse(): PaymentResponse {
        return PaymentResponse(
            transactionId = UUID.randomUUID(),
            status = PaymentStatus.PAYMENT_PENDING,
            amount = BigDecimal("5000.00"),
            currency = "INR",
            paymentUrl = "https://payment.gateway.com/pay/123"
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
