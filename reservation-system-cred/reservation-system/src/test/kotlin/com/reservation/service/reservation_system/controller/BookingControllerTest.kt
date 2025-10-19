package com.reservation.service.reservation_system.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import com.reservation.service.reservation_system.dto.*
import com.reservation.service.reservation_system.exception.SeatBookingInProgressException
import com.reservation.service.reservation_system.handler.BookingHandler
import com.reservation.service.reservation_system.metrics.MetricsService
import com.reservation.service.reservation_system.repository.entity.Ticket
import com.reservation.service.reservation_system.repository.entity.BookingStatus
import com.reservation.service.reservation_system.repository.entity.PaymentStatus
import com.reservation.service.reservation_system.service.FlightScheduleService
import com.reservation.service.reservation_system.service.SeatBookingCacheService
import io.mockk.coEvery
import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

@WebMvcTest(
    controllers = [BookingController::class],
    excludeAutoConfiguration = [SecurityAutoConfiguration::class]
)
class BookingControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockkBean
    private lateinit var bookingHandler: BookingHandler

    @MockkBean
    private lateinit var metricsService: MetricsService

    @MockkBean
    private lateinit var flightScheduleService: FlightScheduleService

    @MockkBean
    private lateinit var seatBookingCacheService: SeatBookingCacheService

    private val userId = UUID.randomUUID()
    private val ticketId = UUID.randomUUID()
    private val seatId = UUID.randomUUID()
    private val vehicleId = UUID.randomUUID()

    @Test
    fun `should book ticket successfully`() {
        // Given
        val bookingRequest = createBookingRequest()
        val expectedTicket = createTicket()

        coEvery { bookingHandler.processBooking(any()) } returns expectedTicket
        every { metricsService.incrementBookingCreated() } returns Unit

        // When & Then
        mockMvc.perform(
            post("/api/book-tickets")
                .header("user-id", userId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(bookingRequest))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.ticketId").value(expectedTicket.ticketId.toString()))
            .andExpect(jsonPath("$.pnrNumber").value(expectedTicket.pnrNumber))
            .andExpect(jsonPath("$.bookingStatus").value(expectedTicket.bookingStatus.name))

        verify { metricsService.incrementBookingCreated() }
    }

    @Test
    fun `should return conflict when seat booking is in progress`() {
        // Given
        val bookingRequest = createBookingRequest()
        val exception = SeatBookingInProgressException("Seat booking in progress, kindly select another seat")

        coEvery { bookingHandler.processBooking(any()) } throws exception
        every { metricsService.incrementApiError() } returns Unit

        // When & Then
        mockMvc.perform(
            post("/api/book-tickets")
                .header("user-id", userId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(bookingRequest))
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.error").value("SEAT_BOOKING_IN_PROGRESS"))
            .andExpect(jsonPath("$.message").value(exception.message))

        verify { metricsService.incrementApiError() }
    }

    @Test
    fun `should return bad request when user-id header is missing`() {
        // Given
        val bookingRequest = createBookingRequest()

        // When & Then
        mockMvc.perform(
            post("/api/book-tickets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(bookingRequest))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `should cancel booking successfully`() {
        // Given
        val bookingId = UUID.randomUUID()
        val bookingType = "FLIGHT"

        every { bookingHandler.processCancellation(bookingId, userId, bookingType) } returns true
        every { metricsService.incrementBookingCancelled() } returns Unit

        // When & Then
        mockMvc.perform(
            delete("/api/book-tickets/$bookingId")
                .header("user-id", userId.toString())
                .param("bookingType", bookingType)
        )
            .andExpect(status().isNoContent)

        verify { bookingHandler.processCancellation(bookingId, userId, bookingType) }
        verify { metricsService.incrementBookingCancelled() }
    }

    @Test
    fun `should return not found when booking cancellation fails`() {
        // Given
        val bookingId = UUID.randomUUID()
        val bookingType = "FLIGHT"

        every { bookingHandler.processCancellation(bookingId, userId, bookingType) } returns false

        // When & Then
        mockMvc.perform(
            delete("/api/book-tickets/$bookingId")
                .header("user-id", userId.toString())
                .param("bookingType", bookingType)
        )
            .andExpect(status().isNotFound)

        verify { bookingHandler.processCancellation(bookingId, userId, bookingType) }
        verify(exactly = 0) { metricsService.incrementApiError() }
        verify(exactly = 0) { metricsService.incrementBookingCancelled() }
    }

    @Test
    fun `should get booking details successfully`() {
        // Given
        val bookingId = UUID.randomUUID()
        val bookingType = "FLIGHT"
        val expectedTicket = createTicket()

        every { bookingHandler.getBookingDetails(bookingId, userId, bookingType) } returns expectedTicket

        // When & Then
        mockMvc.perform(
            get("/api/book-tickets/$bookingId")
                .header("user-id", userId.toString())
                .param("bookingType", bookingType)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.ticketId").value(expectedTicket.ticketId.toString()))
            .andExpect(jsonPath("$.pnrNumber").value(expectedTicket.pnrNumber))

        verify { bookingHandler.getBookingDetails(bookingId, userId, bookingType) }
        verify(exactly = 0) { metricsService.incrementBookingRetrieved() }
    }

    @Test
    fun `should return not found when booking details not found`() {
        // Given
        val bookingId = UUID.randomUUID()
        val bookingType = "FLIGHT"

        every { bookingHandler.getBookingDetails(bookingId, userId, bookingType) } returns null

        // When & Then
        mockMvc.perform(
            get("/api/book-tickets/$bookingId")
                .header("user-id", userId.toString())
                .param("bookingType", bookingType)
        )
            .andExpect(status().isNotFound)

        verify { bookingHandler.getBookingDetails(bookingId, userId, bookingType) }
        verify(exactly = 0) { metricsService.incrementApiError() }
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

    private fun createTicket(): Ticket {
        return Ticket(
            ticketId = ticketId,
            userId = userId,
            pnrNumber = "PNR123456",
            bookingType = BookingType.FLIGHT,
            bookingStatus = BookingStatus.CONFIRMED,
            paymentStatus = PaymentStatus.PAYMENT_PENDING,
            totalAmount = BigDecimal("5000.00"),
            bookingDetails = null,
            flightId = vehicleId.toString(),
            seatId = seatId,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
    }
}
