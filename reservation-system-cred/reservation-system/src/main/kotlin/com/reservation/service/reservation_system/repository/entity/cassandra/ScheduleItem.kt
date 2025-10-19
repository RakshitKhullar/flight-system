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
    val travelEndTime: LocalTime
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
                travelEndTime = this.travelEndTime
            )
        }
}

fun ScheduleItem.getSeatsByStatus(status: SeatStatus): List<SeatInfo> {
    return seats.filter { it.seatStatus == status }
}

fun ScheduleItem.getSeatsByClass(seatClass: SeatClass): List<SeatInfo> {
    return seats.filter { it.seatClass == seatClass }
}

