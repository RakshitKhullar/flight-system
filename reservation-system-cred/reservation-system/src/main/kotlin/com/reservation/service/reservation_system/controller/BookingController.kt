package com.reservation.service.reservation_system.controller

import com.reservation.service.reservation_system.dto.*
import com.reservation.service.reservation_system.exception.SeatBookingInProgressException
import com.reservation.service.reservation_system.handler.BookingHandler
import com.reservation.service.reservation_system.repository.entity.Ticket
import com.reservation.service.reservation_system.metrics.MetricsService
import com.reservation.service.reservation_system.service.FlightScheduleService
import com.reservation.service.reservation_system.service.SeatBookingCacheService
import jakarta.validation.Valid
import kotlinx.coroutines.runBlocking
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.util.UUID

@RestController
@RequestMapping("/api/book-tickets")
class BookingController(
    private val bookingHandler: BookingHandler,
    private val metricsService: MetricsService,
    private val flightScheduleService: FlightScheduleService,
    private val seatBookingCacheService: SeatBookingCacheService
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

    // Flight Management Endpoints
    @PostMapping("/flights")
    fun createFlight(
        @RequestHeader("admin-id") adminId: UUID,
        @Valid @RequestBody flightRequest: FlightCreationRequest
    ): ResponseEntity<Any> {
        return try {
            // Validate the request
            validateFlightRequest(flightRequest)
            
            // Create the flight
            val createdFlight = flightScheduleService.createFlight(flightRequest, adminId)
            
            metricsService.incrementFlightCreated()
            ResponseEntity(createdFlight, HttpStatus.CREATED)
            
        } catch (e: IllegalArgumentException) {
            val errorResponse = mapOf(
                "error" to "INVALID_FLIGHT_DATA",
                "message" to e.message,
                "timestamp" to System.currentTimeMillis()
            )
            ResponseEntity(errorResponse, HttpStatus.BAD_REQUEST)
            
        } catch (e: Exception) {
            metricsService.incrementApiError()
            val errorResponse = mapOf(
                "error" to "FLIGHT_CREATION_FAILED",
                "message" to e.message,
                "timestamp" to System.currentTimeMillis()
            )
            ResponseEntity(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }

    @GetMapping("/flights/search")
    fun searchFlights(
        @RequestParam("sourceCode") sourceCode: String,
        @RequestParam("destinationCode") destinationCode: String,
        @RequestParam("date") date: LocalDate,
        @RequestParam(value = "maxStops", required = false) maxStops: Int? = null,
        @RequestParam(value = "directOnly", required = false) directOnly: Boolean = false
    ): ResponseEntity<List<FlightSearchResponse>> {
        return try {
            val flights = flightScheduleService.searchFlights(
                sourceCode = sourceCode,
                destinationCode = destinationCode,
                date = date,
                maxStops = maxStops,
                directOnly = directOnly
            )
            ResponseEntity.ok(flights)
            
        } catch (e: Exception) {
            metricsService.incrementApiError()
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    @GetMapping("/flights/{flightNumber}")
    fun getFlightDetails(
        @PathVariable flightNumber: String,
        @RequestParam("date") date: LocalDate
    ): ResponseEntity<FlightDetailsResponse> {
        return try {
            val flightDetails = flightScheduleService.getFlightDetails(flightNumber, date)
            if (flightDetails != null) {
                ResponseEntity.ok(flightDetails)
            } else {
                ResponseEntity.notFound().build()
            }
        } catch (e: Exception) {
            metricsService.incrementApiError()
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    @PutMapping("/flights/{flightNumber}/seats/{seatId}/status")
    fun updateSeatStatus(
        @RequestHeader("admin-id") adminId: UUID,
        @PathVariable flightNumber: String,
        @PathVariable seatId: UUID,
        @RequestParam("date") date: LocalDate,
        @RequestBody statusUpdate: SeatStatusUpdateRequest
    ): ResponseEntity<Any> {
        return try {
            val updated = flightScheduleService.updateSeatStatus(
                flightNumber = flightNumber,
                date = date,
                seatId = seatId,
                newStatus = statusUpdate.status,
                adminId = adminId
            )
            
            if (updated) {
                ResponseEntity.ok(mapOf("message" to "Seat status updated successfully"))
            } else {
                ResponseEntity.notFound().build()
            }
        } catch (e: Exception) {
            metricsService.incrementApiError()
            val errorResponse = mapOf(
                "error" to "SEAT_UPDATE_FAILED",
                "message" to e.message,
                "timestamp" to System.currentTimeMillis()
            )
            ResponseEntity(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }

    @DeleteMapping("/flights/{flightNumber}")
    fun cancelFlight(
        @RequestHeader("admin-id") adminId: UUID,
        @PathVariable flightNumber: String,
        @RequestParam("date") date: LocalDate,
        @RequestParam(value = "reason", required = false) reason: String?
    ): ResponseEntity<Any> {
        return try {
            val cancelled = flightScheduleService.cancelFlight(
                flightNumber = flightNumber,
                date = date,
                adminId = adminId,
                reason = reason
            )
            
            if (cancelled) {
                metricsService.incrementFlightCancelled()
                ResponseEntity.ok(mapOf("message" to "Flight cancelled successfully"))
            } else {
                ResponseEntity.notFound().build()
            }
        } catch (e: Exception) {
            metricsService.incrementApiError()
            val errorResponse = mapOf(
                "error" to "FLIGHT_CANCELLATION_FAILED",
                "message" to e.message,
                "timestamp" to System.currentTimeMillis()
            )
            ResponseEntity(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }

    private fun validateFlightRequest(request: FlightCreationRequest) {
        // Validate flight number
        if (request.flightNumber.isBlank()) {
            throw IllegalArgumentException("Flight number cannot be empty")
        }

        // Validate airport codes
        if (request.sourceCode.length != 3 || request.destinationCode.length != 3) {
            throw IllegalArgumentException("Airport codes must be 3 characters long")
        }

        // Validate same source and destination
        if (request.sourceCode == request.destinationCode) {
            throw IllegalArgumentException("Source and destination cannot be the same")
        }

        // Validate times
        if (request.travelStartTime.isAfter(request.travelEndTime)) {
            throw IllegalArgumentException("Start time cannot be after end time")
        }

        // Validate stops
        if (request.numberOfStops != request.stops.size) {
            throw IllegalArgumentException("Number of stops must match stops list size")
        }

        // Validate stop sequences
        if (request.stops.isNotEmpty()) {
            val sequences = request.stops.map { it.stopSequence }.sorted()
            val expectedSequences = (1..request.stops.size).toList()
            if (sequences != expectedSequences) {
                throw IllegalArgumentException("Stop sequences must be consecutive starting from 1")
            }
        }

        // Validate seats
        if (request.seats.isEmpty()) {
            throw IllegalArgumentException("At least one seat must be provided")
        }

        // Validate unique seat numbers
        val seatNumbers = request.seats.map { it.seatNumber }
        if (seatNumbers.size != seatNumbers.distinct().size) {
            throw IllegalArgumentException("Seat numbers must be unique")
        }
    }

    // Debug endpoint to show all segments for a flight
    @GetMapping("/flights/{flightNumber}/segments")
    fun getFlightSegments(
        @PathVariable flightNumber: String,
        @RequestParam("date") date: LocalDate
    ): ResponseEntity<Any> {
        return try {
            val schedules = flightScheduleService.getFlightSchedules(flightNumber)
            val segments = schedules.flatMap { it.schedule }
                .filter { it.date == date }
                .map { segment ->
                    mapOf(
                        "scheduleId" to segment.scheduleId,
                        "route" to "${segment.sourceCode} -> ${segment.destinationCode}",
                        "numberOfStops" to segment.numberOfStops,
                        "isDirect" to segment.isDirect,
                        "availableSeats" to segment.availableSeats,
                        "totalSeats" to segment.totalSeats,
                        "travelTime" to "${segment.travelStartTime} - ${segment.travelEndTime}",
                        "stops" to segment.stops.map { "${it.airportCode} (${it.city})" }
                    )
                }
            
            ResponseEntity.ok(mapOf(
                "flightNumber" to flightNumber,
                "date" to date,
                "totalSegments" to segments.size,
                "segments" to segments
            ))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to "Failed to fetch segments", "message" to e.message))
        }
    }

    // Redis health check endpoint
    @GetMapping("/cache/health")
    fun getCacheHealth(): ResponseEntity<Any> {
        return try {
            val isConnected = seatBookingCacheService.getRedisConnectionStatus()
            val blockedSeatsCount = seatBookingCacheService.getAllBlockedSeats().size
            
            ResponseEntity.ok(mapOf(
                "redis_connected" to isConnected,
                "blocked_seats_count" to blockedSeatsCount,
                "status" to if (isConnected) "healthy" else "unhealthy",
                "timestamp" to System.currentTimeMillis()
            ))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(mapOf(
                    "redis_connected" to false,
                    "status" to "unhealthy",
                    "error" to e.message,
                    "timestamp" to System.currentTimeMillis()
                ))
        }
    }

    // Clear all blocked seats (admin only)
    @DeleteMapping("/cache/blocked-seats")
    fun clearAllBlockedSeats(
        @RequestHeader("admin-id") adminId: UUID
    ): ResponseEntity<Any> {
        return try {
            val beforeCount = seatBookingCacheService.getAllBlockedSeats().size
            seatBookingCacheService.clearAllBlockedSeats()
            val afterCount = seatBookingCacheService.getAllBlockedSeats().size
            
            ResponseEntity.ok(mapOf(
                "message" to "Cleared all blocked seats",
                "seats_cleared" to (beforeCount - afterCount),
                "admin_id" to adminId,
                "timestamp" to System.currentTimeMillis()
            ))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to "Failed to clear blocked seats", "message" to e.message))
        }
    }
}
