package com.reservation.service.reservation_system.controller

import com.reservation.service.reservation_system.service.SeatBookingCacheService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/admin/seat-cache")
class SeatCacheController(
    private val seatBookingCacheService: SeatBookingCacheService
) {

    @GetMapping("/blocked-seats")
    fun getAllBlockedSeats(): ResponseEntity<Map<String, String>> {
        val blockedSeats = seatBookingCacheService.getAllBlockedSeats()
        return ResponseEntity.ok(blockedSeats)
    }

    @GetMapping("/seat-status/{seatKey}")
    fun getSeatStatus(@PathVariable seatKey: String): ResponseEntity<Map<String, Any>> {
        val status = seatBookingCacheService.getSeatBookingStatus(seatKey)
        val response = mapOf(
            "seatKey" to seatKey,
            "status" to (status ?: "AVAILABLE"),
            "isBlocked" to (status == "BLOCKED")
        )
        return ResponseEntity.ok(response)
    }

    @DeleteMapping("/clear-all")
    fun clearAllBlockedSeats(): ResponseEntity<Map<String, String>> {
        seatBookingCacheService.clearAllBlockedSeats()
        return ResponseEntity.ok(mapOf("message" to "All blocked seats cleared successfully"))
    }

    @DeleteMapping("/release/{seatKey}")
    fun releaseSeat(@PathVariable seatKey: String): ResponseEntity<Map<String, String>> {
        seatBookingCacheService.releaseSeatBooking(seatKey)
        return ResponseEntity.ok(mapOf("message" to "Seat released successfully", "seatKey" to seatKey))
    }
}
