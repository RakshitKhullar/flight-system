package com.reservation.service.reservation_system.service

import com.reservation.service.reservation_system.dto.BookingDetails
import com.reservation.service.reservation_system.dto.FlightBookingDetails
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class SeatKeyService {
    
    /**
     * Generate seat key based on booking type using respective service logic
     * This method delegates to specific key generation methods based on booking type
     */
    fun generateSeatKey(bookingDetails: BookingDetails): String {
        return when (bookingDetails) {
            is FlightBookingDetails -> generateFlightSeatKey(bookingDetails)
            // Future: Add support for other booking types
            // is HotelBookingDetails -> generateHotelRoomKey(bookingDetails)
            // is CarRentalBookingDetails -> generateCarKey(bookingDetails)
            else -> throw IllegalArgumentException("Unsupported booking type: ${bookingDetails.getBookingType()}")
        }
    }
    
    private fun generateFlightSeatKey(flightBookingDetails: FlightBookingDetails): String {
        return "${flightBookingDetails.vehicleId}:${flightBookingDetails.seatId}:${flightBookingDetails.flightTime}"
    }
    
    fun generateSeatKey(flightId: String, seatId: UUID, flightTime: String): String {
        return "$flightId:$seatId:$flightTime"
    }
}
