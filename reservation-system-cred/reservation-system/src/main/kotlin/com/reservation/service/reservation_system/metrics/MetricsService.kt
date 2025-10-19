package com.reservation.service.reservation_system.metrics

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class MetricsService(private val meterRegistry: MeterRegistry) {
    
    private val logger = LoggerFactory.getLogger(MetricsService::class.java)

    // Flight Reservation System Counters
    val scheduleCreatedCounter: Counter = Counter.builder("flight_reservation_schedule_created_counter")
        .description("Count of flight schedules created")
        .register(meterRegistry)

    val scheduleRetrievedCounter: Counter = Counter.builder("flight_reservation_schedule_retrieved_counter")
        .description("Count of flight schedules retrieved")
        .register(meterRegistry)

    val scheduleDeletedCounter: Counter = Counter.builder("flight_reservation_schedule_deleted_counter")
        .description("Count of flight schedules deleted")
        .register(meterRegistry)

    val seatStatusUpdatedCounter: Counter = Counter.builder("flight_reservation_seat_status_updated_counter")
        .description("Count of seat status updates")
        .register(meterRegistry)

    val bookingCreatedCounter: Counter = Counter.builder("flight_reservation_booking_created_counter")
        .description("Count of bookings created")
        .register(meterRegistry)

    val bookingCancelledCounter: Counter = Counter.builder("flight_reservation_booking_cancelled_counter")
        .description("Count of bookings cancelled")
        .register(meterRegistry)

    val bookingRetrievedCounter: Counter = Counter.builder("flight_reservation_booking_retrieved_counter")
        .description("Count of bookings retrieved")
        .register(meterRegistry)

    val cityMappingCreatedCounter: Counter = Counter.builder("flight_reservation_city_mapping_created_counter")
        .description("Count of city mappings created")
        .register(meterRegistry)

    val apiErrorCounter: Counter = Counter.builder("flight_reservation_api_error_counter")
        .description("Count of API errors")
        .register(meterRegistry)

    val flightCreatedCounter: Counter = Counter.builder("flight_reservation_flight_created_counter")
        .description("Count of flights created")
        .register(meterRegistry)

    val flightCancelledCounter: Counter = Counter.builder("flight_reservation_flight_cancelled_counter")
        .description("Count of flights cancelled")
        .register(meterRegistry)


    // Metric functions - Simple and Clean
    fun incrementScheduleCreated() {
        try {
            scheduleCreatedCounter.increment()
        } catch (e: Exception) {
            logger.error("Exception in schedule created counter", e)
        }
    }

    fun incrementScheduleRetrieved() {
        try {
            scheduleRetrievedCounter.increment()
        } catch (e: Exception) {
            logger.error("Exception in schedule retrieved counter", e)
        }
    }

    fun incrementScheduleDeleted() {
        try {
            scheduleDeletedCounter.increment()
        } catch (e: Exception) {
            logger.error("Exception in schedule deleted counter", e)
        }
    }

    fun incrementSeatStatusUpdated() {
        try {
            seatStatusUpdatedCounter.increment()
        } catch (e: Exception) {
            logger.error("Exception in seat status updated counter", e)
        }
    }

    fun incrementBookingCreated() {
        try {
            bookingCreatedCounter.increment()
        } catch (e: Exception) {
            logger.error("Exception in booking created counter", e)
        }
    }

    fun incrementBookingCancelled() {
        try {
            bookingCancelledCounter.increment()
        } catch (e: Exception) {
            logger.error("Exception in booking cancelled counter", e)
        }
    }

    fun incrementBookingRetrieved() {
        try {
            bookingRetrievedCounter.increment()
        } catch (e: Exception) {
            logger.error("Exception in booking retrieved counter", e)
        }
    }

    fun incrementCityMappingCreated() {
        try {
            cityMappingCreatedCounter.increment()
        } catch (e: Exception) {
            logger.error("Exception in city mapping created counter", e)
        }
    }

    fun incrementApiError() {
        try {
            apiErrorCounter.increment()
        } catch (e: Exception) {
            logger.error("Exception in API error counter", e)
        }
    }

    fun incrementFlightCreated() {
        try {
            flightCreatedCounter.increment()
        } catch (e: Exception) {
            logger.error("Exception in flight created counter", e)
        }
    }

    fun incrementFlightCancelled() {
        try {
            flightCancelledCounter.increment()
        } catch (e: Exception) {
            logger.error("Exception in flight cancelled counter", e)
        }
    }
}