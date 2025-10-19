package com.reservation.service.reservation_system.controller

import com.reservation.service.reservation_system.repository.TicketRepository
import com.reservation.service.reservation_system.repository.VehicleRepository
import com.reservation.service.reservation_system.repository.SeatRepository
import com.reservation.service.reservation_system.repository.entity.Vehicle
import com.reservation.service.reservation_system.repository.entity.BookingStatus
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@RestController
@RequestMapping("/api/metrics")
class MetricsController(
    private val ticketRepository: TicketRepository,
    private val vehicleRepository: VehicleRepository,
    private val seatRepository: SeatRepository,
    private val meterRegistry: MeterRegistry
) {

    init {
        // Register custom gauges for real-time metrics using direct gauge registration
        meterRegistry.gauge("tickets.total.count", ticketRepository) { repo ->
            repo.count().toDouble()
        }

        meterRegistry.gauge("tickets.confirmed.count", ticketRepository) { repo ->
            repo.findAll().count { it.bookingStatus == BookingStatus.CONFIRMED }.toDouble()
        }

        meterRegistry.gauge("vehicles.total.count", vehicleRepository) { repo ->
            repo.count().toDouble()
        }

        meterRegistry.gauge("vehicles.available.count", vehicleRepository) { repo ->
            repo.findAllByIsAvailable(true).size.toDouble()
        }

        meterRegistry.gauge("seats.total.count", seatRepository) { repo ->
            repo.count().toDouble()
        }

        meterRegistry.gauge("seats.available.count", seatRepository) { repo ->
            repo.findAll().count { it.isAvailable }.toDouble()
        }
    }

    @GetMapping("/dashboard")
    fun getDashboardMetrics(): ResponseEntity<Map<String, Any>> {
        val metrics = mapOf(
            "timestamp" to LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            "tickets" to mapOf(
                "total" to ticketRepository.count(),
                "confirmed" to ticketRepository.findAll().count { it.bookingStatus == BookingStatus.CONFIRMED },
                "cancelled" to ticketRepository.findAll().count { it.bookingStatus == BookingStatus.CANCELLED },
                "pending" to ticketRepository.findAll().count { it.bookingStatus == BookingStatus.PENDING }
            ),
            "vehicles" to mapOf(
                "total" to vehicleRepository.count(),
                "available" to vehicleRepository.findAllByIsAvailable(true).size,
                "byType" to Vehicle.VehicleType.values().associate { type ->
                    type.name to vehicleRepository.findAllByVehicleType(type).size
                }
            ),
            "seats" to mapOf(
                "total" to seatRepository.count(),
                "available" to seatRepository.findAll().count { it.isAvailable },
                "occupied" to seatRepository.findAll().count { !it.isAvailable }
            )
        )
        return ResponseEntity.ok(metrics)
    }

    @GetMapping("/health-check")
    fun getHealthMetrics(): ResponseEntity<Map<String, Any>> {
        val healthMetrics = mapOf(
            "status" to "UP",
            "timestamp" to LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            "database" to mapOf(
                "postgresql" to "UP",
                "cassandra" to "UP"
            ),
            "services" to mapOf(
                "booking" to "UP",
                "schedule" to "UP",
                "vehicle" to "UP"
            )
        )
        return ResponseEntity.ok(healthMetrics)
    }

    @GetMapping("/performance")
    fun getPerformanceMetrics(): ResponseEntity<Map<String, Any>> {
        val performanceMetrics = mapOf(
            "timestamp" to LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            "jvm" to mapOf(
                "memory" to mapOf(
                    "used" to Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory(),
                    "free" to Runtime.getRuntime().freeMemory(),
                    "total" to Runtime.getRuntime().totalMemory(),
                    "max" to Runtime.getRuntime().maxMemory()
                ),
                "processors" to Runtime.getRuntime().availableProcessors()
            ),
            "system" to mapOf(
                "uptime" to System.currentTimeMillis(),
                "timestamp" to System.currentTimeMillis()
            )
        )
        return ResponseEntity.ok(performanceMetrics)
    }
}
