package com.reservation.service.reservation_system.controller

import com.reservation.service.reservation_system.service.BookingStatusService
import com.reservation.service.reservation_system.service.SeatBookingCacheService
import com.reservation.service.reservation_system.service.SeatKeyService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/booking-status")
class BookingStatusController(
    private val bookingStatusService: BookingStatusService,
    private val seatBookingCacheService: SeatBookingCacheService,
    private val seatKeyService: SeatKeyService
) {
    
    private val logger = LoggerFactory.getLogger(BookingStatusController::class.java)

    @PutMapping("/release-seat")
    fun releaseSeatAndUpdateStatus(
        @RequestParam flightId: String,
        @RequestParam seatId: UUID,
        @RequestParam flightTime: String
    ): ResponseEntity<Map<String, Any>> {
        
        logger.info("Releasing seat and updating status - FlightId: $flightId, SeatId: $seatId, FlightTime: $flightTime")
        
        return try {
            // Step 1: Generate seat key
            val seatKey = seatKeyService.generateSeatKey(flightId, seatId, flightTime)
            logger.debug("Generated seat key: $seatKey")
            
            // Step 2: Update booking status to AVAILABLE in NoSQL database
            val updated = bookingStatusService.updateSeatStatusToAvailable(flightId, seatId)
            
            if (updated) {
                // Step 3: Clear cache entry for this seat
                seatBookingCacheService.releaseSeatBooking(seatKey)
                logger.info("Successfully released seat and updated status for key: $seatKey")
                
                val response = mapOf(
                    "success" to true,
                    "message" to "Seat released and status updated to AVAILABLE",
                    "flightId" to flightId,
                    "seatId" to seatId.toString(),
                    "flightTime" to flightTime,
                    "seatKey" to seatKey,
                    "timestamp" to System.currentTimeMillis()
                )
                ResponseEntity.ok(response)
                
            } else {
                logger.warn("Failed to update seat status in database for key: $seatKey")
                val response = mapOf(
                    "success" to false,
                    "message" to "Failed to update seat status in database",
                    "flightId" to flightId,
                    "seatId" to seatId.toString(),
                    "timestamp" to System.currentTimeMillis()
                )
                ResponseEntity.badRequest().body(response)
            }
            
        } catch (e: Exception) {
            logger.error("Error releasing seat and updating status", e)
            val response = mapOf(
                "success" to false,
                "message" to "Error: ${e.message}",
                "flightId" to flightId,
                "seatId" to seatId.toString(),
                "timestamp" to System.currentTimeMillis()
            )
            ResponseEntity.internalServerError().body(response)
        }
    }

    @PutMapping("/release-seat-by-key")
    fun releaseSeatByKey(
        @RequestParam seatKey: String
    ): ResponseEntity<Map<String, Any>> {
        
        logger.info("Releasing seat by key: $seatKey")
        
        return try {
            // Parse seat key to extract components
            val keyParts = seatKey.split(":")
            if (keyParts.size != 3) {
                throw IllegalArgumentException("Invalid seat key format. Expected: flightId:seatId:flightTime")
            }
            
            val flightId = keyParts[0]
            val seatId = UUID.fromString(keyParts[1])
            val flightTime = keyParts[2]
            
            // Step 1: Update booking status to AVAILABLE in NoSQL database
            val updated = bookingStatusService.updateSeatStatusToAvailable(flightId, seatId)
            
            if (updated) {
                // Step 2: Clear cache entry
                seatBookingCacheService.releaseSeatBooking(seatKey)
                logger.info("Successfully released seat by key: $seatKey")
                
                val response = mapOf(
                    "success" to true,
                    "message" to "Seat released and status updated to AVAILABLE",
                    "seatKey" to seatKey,
                    "flightId" to flightId,
                    "seatId" to seatId.toString(),
                    "flightTime" to flightTime,
                    "timestamp" to System.currentTimeMillis()
                )
                ResponseEntity.ok(response)
                
            } else {
                logger.warn("Failed to update seat status in database for key: $seatKey")
                val response = mapOf(
                    "success" to false,
                    "message" to "Failed to update seat status in database",
                    "seatKey" to seatKey,
                    "timestamp" to System.currentTimeMillis()
                )
                ResponseEntity.badRequest().body(response)
            }
            
        } catch (e: Exception) {
            logger.error("Error releasing seat by key: $seatKey", e)
            val response = mapOf(
                "success" to false,
                "message" to "Error: ${e.message}",
                "seatKey" to seatKey,
                "timestamp" to System.currentTimeMillis()
            )
            ResponseEntity.internalServerError().body(response)
        }
    }

    @GetMapping("/seat-status")
    fun getSeatStatus(
        @RequestParam flightId: String,
        @RequestParam seatId: UUID,
        @RequestParam flightTime: String
    ): ResponseEntity<Map<String, Any>> {
        
        return try {
            val seatKey = seatKeyService.generateSeatKey(flightId, seatId, flightTime)
            val cacheStatus = seatBookingCacheService.getSeatBookingStatus(seatKey)
            val dbStatus = bookingStatusService.getSeatStatus(flightId, seatId)
            
            val response = mapOf(
                "seatKey" to seatKey,
                "flightId" to flightId,
                "seatId" to seatId.toString(),
                "flightTime" to flightTime,
                "cacheStatus" to (cacheStatus ?: "NOT_IN_CACHE"),
                "databaseStatus" to (dbStatus ?: "NOT_FOUND"),
                "isBlocked" to (cacheStatus == "BLOCKED"),
                "timestamp" to System.currentTimeMillis()
            )
            ResponseEntity.ok(response)
            
        } catch (e: Exception) {
            logger.error("Error getting seat status", e)
            val response = mapOf(
                "success" to false,
                "message" to "Error: ${e.message}",
                "timestamp" to System.currentTimeMillis()
            )
            ResponseEntity.internalServerError().body(response)
        }
    }

    @PutMapping("/unblock-seat")
    fun unblockSeat(
        @RequestParam flightId: String,
        @RequestParam seatId: UUID,
        @RequestParam flightTime: String
    ): ResponseEntity<Map<String, Any>> {
        
        logger.info("Unblocking particular seat - FlightId: $flightId, SeatId: $seatId, FlightTime: $flightTime")
        
        return try {
            // Step 1: Generate seat key
            val seatKey = seatKeyService.generateSeatKey(flightId, seatId, flightTime)
            logger.debug("Generated seat key for unblocking: $seatKey")
            
            // Step 2: Check if seat is currently blocked in cache
            val isBlocked = seatBookingCacheService.isSeatBookingInProgress(seatKey)
            if (!isBlocked) {
                logger.warn("Seat is not currently blocked in cache: $seatKey")
                val response = mapOf(
                    "success" to false,
                    "message" to "Seat is not currently blocked",
                    "flightId" to flightId,
                    "seatId" to seatId.toString(),
                    "flightTime" to flightTime,
                    "seatKey" to seatKey,
                    "timestamp" to System.currentTimeMillis()
                )
                return ResponseEntity.badRequest().body(response)
            }
            
            // Step 3: Update seat status to AVAILABLE in NoSQL database
            val updated = bookingStatusService.updateSeatStatusToAvailable(flightId, seatId)
            
            if (updated) {
                // Step 4: Clear cache entry for this specific seat
                seatBookingCacheService.releaseSeatBooking(seatKey)
                logger.info("Successfully unblocked seat: $seatKey")
                
                val response = mapOf(
                    "success" to true,
                    "message" to "Seat successfully unblocked and status updated to AVAILABLE",
                    "flightId" to flightId,
                    "seatId" to seatId.toString(),
                    "flightTime" to flightTime,
                    "seatKey" to seatKey,
                    "previousStatus" to "BLOCKED",
                    "currentStatus" to "AVAILABLE",
                    "timestamp" to System.currentTimeMillis()
                )
                ResponseEntity.ok(response)
                
            } else {
                logger.warn("Failed to update seat status in database for key: $seatKey")
                val response = mapOf(
                    "success" to false,
                    "message" to "Failed to update seat status in database",
                    "flightId" to flightId,
                    "seatId" to seatId.toString(),
                    "seatKey" to seatKey,
                    "timestamp" to System.currentTimeMillis()
                )
                ResponseEntity.internalServerError().body(response)
            }
            
        } catch (e: Exception) {
            logger.error("Error unblocking seat", e)
            val response = mapOf(
                "success" to false,
                "message" to "Error: ${e.message}",
                "flightId" to flightId,
                "seatId" to seatId.toString(),
                "timestamp" to System.currentTimeMillis()
            )
            ResponseEntity.internalServerError().body(response)
        }
    }
}
