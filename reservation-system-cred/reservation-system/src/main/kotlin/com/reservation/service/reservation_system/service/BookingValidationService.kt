package com.reservation.service.reservation_system.service

import com.reservation.service.reservation_system.dto.FlightBookingDetails
import com.reservation.service.reservation_system.repository.entity.cassandra.SeatInfo
import com.reservation.service.reservation_system.repository.entity.cassandra.TravelSchedule

interface BookingValidationService {
    fun validateBookingRequest(flightDetails: FlightBookingDetails): ValidationResult
    fun findAvailableSeat(travelSchedule: TravelSchedule, flightDetails: FlightBookingDetails): SeatInfo?
    fun validateSeatAvailability(seat: SeatInfo): ValidationResult
}

data class ValidationResult(
    val isValid: Boolean,
    val errorMessage: String? = null
) {
    companion object {
        fun success() = ValidationResult(true)
        fun failure(message: String) = ValidationResult(false, message)
    }
}
