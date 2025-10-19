package com.reservation.service.reservation_system.controller

import com.reservation.service.reservation_system.dto.BookingRequest
import com.reservation.service.reservation_system.exception.SeatBookingInProgressException
import com.reservation.service.reservation_system.handler.BookingHandler
import com.reservation.service.reservation_system.repository.entity.Ticket
import com.reservation.service.reservation_system.metrics.MetricsService
import kotlinx.coroutines.runBlocking
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/book-tickets")
class BookingController(
    private val bookingHandler: BookingHandler,
    private val metricsService: MetricsService
) {

    @PostMapping
    fun bookTicket(
        @RequestHeader("user-id") userId: UUID,
        @RequestBody bookingRequest: BookingRequest
    ): ResponseEntity<Any> {
        return try {
            // Set userId from header to booking request
            val updatedBookingRequest = bookingRequest.copy(userId = userId)
            
            // Use runBlocking to handle suspend function
            val ticket = runBlocking {
                bookingHandler.processBooking(updatedBookingRequest)
            }
            
            metricsService.incrementBookingCreated()
            ResponseEntity(ticket, HttpStatus.CREATED)
            
        } catch (e: SeatBookingInProgressException) {
            metricsService.incrementApiError()
            val errorResponse = mapOf(
                "error" to "SEAT_BOOKING_IN_PROGRESS",
                "message" to e.message,
                "timestamp" to System.currentTimeMillis()
            )
            ResponseEntity(errorResponse, HttpStatus.CONFLICT)
            
        } catch (e: Exception) {
            metricsService.incrementApiError()
            throw e
        }
    }

    @DeleteMapping("/{bookingId}")
    fun cancelBooking(
        @RequestHeader("user-id") userId: UUID,
        @PathVariable bookingId: UUID,
        @RequestParam("bookingType") bookingType: String
    ): ResponseEntity<Void> {
        val result = bookingHandler.processCancellation(bookingId, userId, bookingType)
        if (result) {
            metricsService.incrementBookingCancelled()
            return ResponseEntity.noContent().build()
        } else {
            return ResponseEntity.notFound().build()
        }
    }

    @GetMapping("/{bookingId}")
    fun getBookingDetails(
        @RequestHeader("user-id") userId: UUID,
        @PathVariable bookingId: UUID,
        @RequestParam("bookingType") bookingType: String
    ): ResponseEntity<Ticket> {
        val ticket = bookingHandler.getBookingDetails(bookingId, userId, bookingType)
        return if (ticket != null) {
            ResponseEntity.ok(ticket)
        } else {
            ResponseEntity.notFound().build()
        }
    }
}
