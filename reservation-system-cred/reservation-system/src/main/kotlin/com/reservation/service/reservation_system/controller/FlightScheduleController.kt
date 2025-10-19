package com.reservation.service.reservation_system.controller

import com.reservation.service.reservation_system.repository.entity.cassandra.TravelSchedule
import com.reservation.service.reservation_system.repository.entity.cassandra.ScheduleItem
import com.reservation.service.reservation_system.repository.entity.cassandra.SeatInfo
import com.reservation.service.reservation_system.repository.entity.cassandra.SeatStatus
import com.reservation.service.reservation_system.service.FlightScheduleService
import com.reservation.service.reservation_system.metrics.MetricsService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/flight-schedules")
class FlightScheduleController(
    private val flightScheduleService: FlightScheduleService,
    private val metricsService: MetricsService
) {

    @PostMapping
    fun createFlightSchedule(
        @RequestParam flightId: String,
        @RequestBody scheduleItems: List<ScheduleItem>
    ): ResponseEntity<TravelSchedule> {
        return try {
            val schedule = flightScheduleService.createFlightSchedule(flightId, scheduleItems)
            metricsService.incrementScheduleCreated()
            ResponseEntity(schedule, HttpStatus.CREATED)
        } catch (e: Exception) {
            metricsService.incrementApiError()
            throw e
        }
    }

    @GetMapping("/flight/{flightId}")
    fun getFlightSchedules(@PathVariable flightId: String): ResponseEntity<List<TravelSchedule>> {
        val schedules = flightScheduleService.getFlightSchedules(flightId)
        metricsService.incrementScheduleRetrieved()
        return ResponseEntity.ok(schedules)
    }

    @GetMapping
    fun getAllFlightSchedules(): ResponseEntity<List<TravelSchedule>> {
        val schedules = flightScheduleService.getAllFlightSchedules()
        metricsService.incrementScheduleRetrieved()
        return ResponseEntity.ok(schedules)
    }

    /**
     * this is the backdoor api
     * when customer request to book the seat then we block it and initiate it for payment
     * since there is no payment service so we have make this api to update the seat status to BOOKED
     */

    @PutMapping("/flight/{flightId}/seat/{seatId}/status")
    fun updateSeatStatus(
        @PathVariable flightId: String,
        @PathVariable seatId: UUID,
        @RequestParam status: SeatStatus
    ): ResponseEntity<TravelSchedule> {
        val result = flightScheduleService.updateSeatStatusByFlightId(flightId, seatId, status)
        return if (result != null) {
            metricsService.incrementSeatStatusUpdated()
            ResponseEntity.ok(result)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @PostMapping("/flight/{flightId}/seat/{seatId}/confirm")
    fun confirmBlockedSeat(
        @PathVariable flightId: String,
        @PathVariable seatId: UUID
    ): ResponseEntity<TravelSchedule> {
        return try {
            val updatedSchedule = flightScheduleService.confirmBlockedSeatByFlightId(flightId, seatId)
            if (updatedSchedule != null) {
                metricsService.incrementSeatStatusUpdated()
                ResponseEntity.ok(updatedSchedule)
            } else {
                ResponseEntity.notFound().build()
            }
        } catch (e: IllegalStateException) {
            ResponseEntity.badRequest().build()
        }
    }

    @GetMapping("/blocked-seats/{flightId}")
    fun getBlockedSeats(@PathVariable flightId: String): ResponseEntity<List<SeatInfo>> {
        val blockedSeats = flightScheduleService.getBlockedSeats(flightId)
        metricsService.incrementScheduleRetrieved()
        return ResponseEntity.ok(blockedSeats)
    }

    @DeleteMapping("/{scheduleId}")
    fun deleteFlightSchedule(@PathVariable scheduleId: UUID): ResponseEntity<Void> {
        val deleted = flightScheduleService.deleteFlightSchedule(scheduleId)
        if (deleted) {
            metricsService.incrementScheduleDeleted()
            return ResponseEntity.noContent().build()
        } else {
            return ResponseEntity.notFound().build()
        }
    }
}
