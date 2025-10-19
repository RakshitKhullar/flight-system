package com.reservation.service.reservation_system.controller

import com.reservation.service.reservation_system.repository.entity.Seat
import com.reservation.service.reservation_system.service.SeatService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/seats")
class SeatController(
    private val seatService: SeatService
) {

    @PostMapping
    fun createSeat(
        @RequestParam vehicleId: UUID,
        @RequestParam seatNumber: String,
        @RequestParam seatType: Seat.SeatType,
        @RequestParam(defaultValue = "false") isWindowSeat: Boolean
    ): ResponseEntity<Seat> {
        return seatService.createSeat(vehicleId, seatNumber, seatType, isWindowSeat)
            ?.let { ResponseEntity(it, HttpStatus.CREATED) }
            ?: ResponseEntity.badRequest().build()
    }

    @GetMapping("/vehicle/{vehicleId}")
    fun getSeatsByVehicle(@PathVariable vehicleId: UUID): ResponseEntity<List<Seat>> {
        val seats = seatService.getSeatsByVehicle(vehicleId)
        return ResponseEntity.ok(seats)
    }

    @GetMapping("/vehicle/{vehicleId}/available")
    fun getAvailableSeatsByVehicle(@PathVariable vehicleId: UUID): ResponseEntity<List<Seat>> {
        val seats = seatService.getAvailableSeatsByVehicle(vehicleId)
        return ResponseEntity.ok(seats)
    }

    @GetMapping("/vehicle/{vehicleId}/window")
    fun getWindowSeatsByVehicle(@PathVariable vehicleId: UUID): ResponseEntity<List<Seat>> {
        val seats = seatService.getWindowSeatsByVehicle(vehicleId)
        return ResponseEntity.ok(seats)
    }

    @GetMapping("/vehicle/{vehicleId}/type/{seatType}")
    fun getSeatsByTypeAndVehicle(
        @PathVariable vehicleId: UUID,
        @PathVariable seatType: Seat.SeatType
    ): ResponseEntity<List<Seat>> {
        val seats = seatService.getSeatsByTypeAndVehicle(vehicleId, seatType)
        return ResponseEntity.ok(seats)
    }

    @GetMapping("/vehicle/{vehicleId}/seat/{seatNumber}")
    fun getSeatByNumberAndVehicle(
        @PathVariable vehicleId: UUID,
        @PathVariable seatNumber: String
    ): ResponseEntity<Seat> {
        return seatService.getSeatByNumberAndVehicle(seatNumber, vehicleId)
            ?.let { ResponseEntity.ok(it) }
            ?: ResponseEntity.notFound().build()
    }

    @GetMapping("/vehicle/{vehicleId}/count/available")
    fun getAvailableSeatCount(@PathVariable vehicleId: UUID): ResponseEntity<Map<String, Long>> {
        val count = seatService.getAvailableSeatCount(vehicleId)
        return ResponseEntity.ok(mapOf("availableSeats" to count))
    }

    @PutMapping("/{seatId}/availability")
    fun updateSeatAvailability(
        @PathVariable seatId: Long,
        @RequestParam isAvailable: Boolean
    ): ResponseEntity<Seat> {
        return seatService.updateSeatAvailability(seatId, isAvailable)
            ?.let { ResponseEntity.ok(it) }
            ?: ResponseEntity.notFound().build()
    }

    @DeleteMapping("/{seatId}")
    fun deleteSeat(@PathVariable seatId: Long): ResponseEntity<Void> {
        return if (seatService.deleteSeat(seatId)) {
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }
}
