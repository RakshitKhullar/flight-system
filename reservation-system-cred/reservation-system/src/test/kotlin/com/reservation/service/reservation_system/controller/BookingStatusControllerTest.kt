package com.reservation.service.reservation_system.controller

import com.ninjasquad.springmockk.MockkBean
import com.reservation.service.reservation_system.service.BookingStatusService
import com.reservation.service.reservation_system.service.SeatBookingCacheService
import com.reservation.service.reservation_system.service.SeatKeyService
import io.mockk.every
import io.mockk.verify
import org.hamcrest.Matchers
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.util.*

@WebMvcTest(BookingStatusController::class)
class BookingStatusControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var bookingStatusService: BookingStatusService

    @MockkBean
    private lateinit var seatBookingCacheService: SeatBookingCacheService

    @MockkBean
    private lateinit var seatKeyService: SeatKeyService

    private val flightId = "FL123"
    private val seatId = UUID.fromString("7ecb0897-2196-4199-8953-13de19b84001")
    private val flightTime = "1030"  // Changed to avoid colon in time
    private val seatKey = "$flightId:$seatId:$flightTime"

    @Test
    fun `should release seat and update status successfully`() {
        // Given
        every { seatKeyService.generateSeatKey(flightId, seatId, flightTime) } returns seatKey
        every { bookingStatusService.updateSeatStatusToAvailable(flightId, seatId) } returns true
        every { seatBookingCacheService.releaseSeatBooking(seatKey) } returns Unit

        // When & Then
        mockMvc.perform(
            put("/api/booking-status/release-seat")
                .param("flightId", flightId)
                .param("seatId", seatId.toString())
                .param("flightTime", flightTime)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("Seat released and status updated to AVAILABLE"))
            .andExpect(jsonPath("$.flightId").value(flightId))
            .andExpect(jsonPath("$.seatId").value(seatId.toString()))
            .andExpect(jsonPath("$.seatKey").value(seatKey))

        verify { seatKeyService.generateSeatKey(flightId, seatId, flightTime) }
        verify { bookingStatusService.updateSeatStatusToAvailable(flightId, seatId) }
        verify { seatBookingCacheService.releaseSeatBooking(seatKey) }
    }

    @Test
    fun `should return bad request when database update fails`() {
        // Given
        every { seatKeyService.generateSeatKey(flightId, seatId, flightTime) } returns seatKey
        every { bookingStatusService.updateSeatStatusToAvailable(flightId, seatId) } returns false

        // When & Then
        mockMvc.perform(
            put("/api/booking-status/release-seat")
                .param("flightId", flightId)
                .param("seatId", seatId.toString())
                .param("flightTime", flightTime)
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").value("Failed to update seat status in database"))

        verify { bookingStatusService.updateSeatStatusToAvailable(flightId, seatId) }
        verify(exactly = 0) { seatBookingCacheService.releaseSeatBooking(any()) }
    }

    @Test
    fun `should release seat by key successfully`() {
        // Given
        every { bookingStatusService.updateSeatStatusToAvailable(flightId, seatId) } returns true
        every { seatBookingCacheService.releaseSeatBooking(seatKey) } returns Unit

        // When & Then
        mockMvc.perform(
            put("/api/booking-status/release-seat-by-key")
                .param("seatKey", seatKey)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("Seat released and status updated to AVAILABLE"))
            .andExpect(jsonPath("$.seatKey").value(seatKey))

        verify { bookingStatusService.updateSeatStatusToAvailable(flightId, seatId) }
        verify { seatBookingCacheService.releaseSeatBooking(seatKey) }
    }

    @Test
    fun `should return bad request for invalid seat key format`() {
        // Given
        val invalidSeatKey = "invalid:key"

        // When & Then
        mockMvc.perform(
            put("/api/booking-status/release-seat-by-key")
                .param("seatKey", invalidSeatKey)
        )
            .andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").value(Matchers.containsString("Invalid seat key format")))
    }

    @Test
    fun `should unblock seat successfully when seat is blocked`() {
        // Given
        every { seatKeyService.generateSeatKey(flightId, seatId, flightTime) } returns seatKey
        every { seatBookingCacheService.isSeatBookingInProgress(seatKey) } returns true
        every { bookingStatusService.updateSeatStatusToAvailable(flightId, seatId) } returns true
        every { seatBookingCacheService.releaseSeatBooking(seatKey) } returns Unit

        // When & Then
        mockMvc.perform(
            put("/api/booking-status/unblock-seat")
                .param("flightId", flightId)
                .param("seatId", seatId.toString())
                .param("flightTime", flightTime)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("Seat successfully unblocked and status updated to AVAILABLE"))
            .andExpect(jsonPath("$.previousStatus").value("BLOCKED"))
            .andExpect(jsonPath("$.currentStatus").value("AVAILABLE"))

        verify { seatBookingCacheService.isSeatBookingInProgress(seatKey) }
        verify { bookingStatusService.updateSeatStatusToAvailable(flightId, seatId) }
        verify { seatBookingCacheService.releaseSeatBooking(seatKey) }
    }

    @Test
    fun `should return bad request when seat is not blocked`() {
        // Given
        every { seatKeyService.generateSeatKey(flightId, seatId, flightTime) } returns seatKey
        every { seatBookingCacheService.isSeatBookingInProgress(seatKey) } returns false

        // When & Then
        mockMvc.perform(
            put("/api/booking-status/unblock-seat")
                .param("flightId", flightId)
                .param("seatId", seatId.toString())
                .param("flightTime", flightTime)
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").value("Seat is not currently blocked"))

        verify { seatBookingCacheService.isSeatBookingInProgress(seatKey) }
        verify(exactly = 0) { bookingStatusService.updateSeatStatusToAvailable(any(), any()) }
    }

    @Test
    fun `should get seat status successfully`() {
        // Given
        val cacheStatus = "BLOCKED"
        val dbStatus = "AVAILABLE"

        every { seatKeyService.generateSeatKey(flightId, seatId, flightTime) } returns seatKey
        every { seatBookingCacheService.getSeatBookingStatus(seatKey) } returns cacheStatus
        every { bookingStatusService.getSeatStatus(flightId, seatId) } returns dbStatus

        // When & Then
        mockMvc.perform(
            get("/api/booking-status/seat-status")
                .param("flightId", flightId)
                .param("seatId", seatId.toString())
                .param("flightTime", flightTime)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.seatKey").value(seatKey))
            .andExpect(jsonPath("$.cacheStatus").value(cacheStatus))
            .andExpect(jsonPath("$.databaseStatus").value(dbStatus))
            .andExpect(jsonPath("$.isBlocked").value(true))

        verify { seatKeyService.generateSeatKey(flightId, seatId, flightTime) }
        verify { seatBookingCacheService.getSeatBookingStatus(seatKey) }
        verify { bookingStatusService.getSeatStatus(flightId, seatId) }
    }

    @Test
    fun `should get seat status with null cache status`() {
        // Given
        val dbStatus = "AVAILABLE"

        every { seatKeyService.generateSeatKey(flightId, seatId, flightTime) } returns seatKey
        every { seatBookingCacheService.getSeatBookingStatus(seatKey) } returns null
        every { bookingStatusService.getSeatStatus(flightId, seatId) } returns dbStatus

        // When & Then
        mockMvc.perform(
            get("/api/booking-status/seat-status")
                .param("flightId", flightId)
                .param("seatId", seatId.toString())
                .param("flightTime", flightTime)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.cacheStatus").value("NOT_IN_CACHE"))
            .andExpect(jsonPath("$.databaseStatus").value(dbStatus))
            .andExpect(jsonPath("$.isBlocked").value(false))
    }

    @Test
    fun `should handle missing required parameters`() {
        // When & Then
        mockMvc.perform(
            put("/api/booking-status/release-seat")
                .param("flightId", flightId)
                // Missing seatId and flightTime
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `should handle invalid UUID format`() {
        // When & Then
        mockMvc.perform(
            put("/api/booking-status/release-seat")
                .param("flightId", flightId)
                .param("seatId", "invalid-uuid")
                .param("flightTime", flightTime)
        )
            .andExpect(status().isBadRequest)
    }
}
