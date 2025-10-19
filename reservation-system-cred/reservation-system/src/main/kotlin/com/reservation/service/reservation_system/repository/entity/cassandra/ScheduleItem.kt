package com.reservation.service.reservation_system.repository.entity.cassandra

import org.springframework.data.cassandra.core.mapping.UserDefinedType
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

@UserDefinedType("schedule_item")
data class ScheduleItem(
    val scheduleId: UUID = UUID.randomUUID(),
    val date: LocalDate,
    val flightNumber: String,
    val sourceCode: String,
    val destinationCode: String,
    val travelStartTime: LocalTime,
    val travelEndTime: LocalTime,
    val seats: List<SeatInfo> = emptyList(),
    val totalSeats: Int = 0,
    val availableSeats: Int = 0,
    val numberOfStops: Int = 0,
    val stops: List<FlightStop> = emptyList(),
    val isDirect: Boolean = numberOfStops == 0
)

@UserDefinedType("flight_stop")
data class FlightStop(
    val stopId: UUID = UUID.randomUUID(),
    val airportCode: String,
    val airportName: String,
    val city: String,
    val arrivalTime: LocalTime,
    val departureTime: LocalTime,
    val layoverDuration: Int, // in minutes
    val stopSequence: Int // 1 for first stop, 2 for second stop, etc.
)

@UserDefinedType("seat_info")
data class SeatInfo(
    val seatId: UUID = UUID.randomUUID(),
    val seatNumber: String,
    val seatClass: SeatClass,
    val amount: BigDecimal,
    val seatStatus: SeatStatus
)

@UserDefinedType("available_seat")
data class AvailableSeat(
    val seatId: UUID,
    val seatNumber: String,
    val seatClass: SeatClass,
    val amount: BigDecimal,
    val scheduleId: UUID,
    val date: LocalDate,
    val flightNumber: String,
    val sourceCode: String,
    val destinationCode: String,
    val travelStartTime: LocalTime,
    val travelEndTime: LocalTime,
    val numberOfStops: Int = 0,
    val stops: List<FlightStop> = emptyList(),
    val isDirect: Boolean = numberOfStops == 0
)

enum class SeatStatus {
    AVAILABLE,
    BOOKED,
    BLOCKED,
    MAINTENANCE
}

enum class SeatClass {
    ECONOMY,
    PREMIUM_ECONOMY,
    BUSINESS,
    FIRST_CLASS
}

// Extension functions for convenience
fun ScheduleItem.getAvailableSeats(): List<AvailableSeat> {
    return seats.filter { it.seatStatus == SeatStatus.AVAILABLE }
        .map { seatInfo ->
            AvailableSeat(
                seatId = seatInfo.seatId,
                seatNumber = seatInfo.seatNumber,
                seatClass = seatInfo.seatClass,
                amount = seatInfo.amount,
                scheduleId = this.scheduleId,
                date = this.date,
                flightNumber = this.flightNumber,
                sourceCode = this.sourceCode,
                destinationCode = this.destinationCode,
                travelStartTime = this.travelStartTime,
                travelEndTime = this.travelEndTime,
                numberOfStops = this.numberOfStops,
                stops = this.stops,
                isDirect = this.isDirect
            )
        }
}

fun ScheduleItem.getSeatsByStatus(status: SeatStatus): List<SeatInfo> {
    return seats.filter { it.seatStatus == status }
}

fun ScheduleItem.getSeatsByClass(seatClass: SeatClass): List<SeatInfo> {
    return seats.filter { it.seatClass == seatClass }
}

// Extension functions for stops
fun ScheduleItem.getTotalTravelTime(): Int {
    // Calculate total travel time in minutes including layovers
    val directTravelTime = java.time.Duration.between(travelStartTime, travelEndTime).toMinutes().toInt()
    return directTravelTime
}

fun ScheduleItem.getTotalLayoverTime(): Int {
    return stops.sumOf { it.layoverDuration }
}

fun ScheduleItem.getStopsBySequence(): List<FlightStop> {
    return stops.sortedBy { it.stopSequence }
}

fun ScheduleItem.hasStopInCity(city: String): Boolean {
    return stops.any { it.city.equals(city, ignoreCase = true) }
}

fun ScheduleItem.hasStopAtAirport(airportCode: String): Boolean {
    return stops.any { it.airportCode.equals(airportCode, ignoreCase = true) }
}

fun ScheduleItem.getFlightType(): FlightType {
    return when (numberOfStops) {
        0 -> FlightType.DIRECT
        1 -> FlightType.ONE_STOP
        2 -> FlightType.TWO_STOPS
        else -> FlightType.MULTI_STOP
    }
}

enum class FlightType {
    DIRECT,
    ONE_STOP,
    TWO_STOPS,
    MULTI_STOP
}

