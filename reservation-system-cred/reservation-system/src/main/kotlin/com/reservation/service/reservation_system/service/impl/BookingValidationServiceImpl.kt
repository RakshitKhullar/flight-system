package com.reservation.service.reservation_system.service.impl

import com.reservation.service.reservation_system.dto.FlightBookingDetails
import com.reservation.service.reservation_system.repository.entity.cassandra.SeatInfo
import com.reservation.service.reservation_system.repository.entity.cassandra.SeatStatus
import com.reservation.service.reservation_system.repository.entity.cassandra.TravelSchedule
import com.reservation.service.reservation_system.service.BookingValidationService
import com.reservation.service.reservation_system.service.ValidationResult
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class BookingValidationServiceImpl : BookingValidationService {
    
    private val logger = LoggerFactory.getLogger(BookingValidationServiceImpl::class.java)
    
    override fun validateBookingRequest(flightDetails: FlightBookingDetails): ValidationResult {
        return when {
            flightDetails.sourceCode.isBlank() -> 
                ValidationResult.failure("Source code cannot be empty")
            
            flightDetails.destinationCode.isBlank() -> 
                ValidationResult.failure("Destination code cannot be empty")
            
            flightDetails.sourceCode == flightDetails.destinationCode -> 
                ValidationResult.failure("Source and destination cannot be same")
            
            else -> ValidationResult.success()
        }
    }
    
    override fun findAvailableSeat(travelSchedule: TravelSchedule, flightDetails: FlightBookingDetails): SeatInfo? {
        return travelSchedule.schedule
            .flatMap { scheduleItem -> 
                scheduleItem.seats.filter { seat ->
                    seat.seatId == flightDetails.seatId &&
                    scheduleItem.sourceCode == flightDetails.sourceCode &&
                    scheduleItem.destinationCode == flightDetails.destinationCode
                }
            }
            .firstOrNull()
    }
    
    override fun validateSeatAvailability(seat: SeatInfo): ValidationResult {
        return when (seat.seatStatus) {
            SeatStatus.AVAILABLE -> {
                logger.info("Seat ${seat.seatNumber} is available for booking")
                ValidationResult.success()
            }
            SeatStatus.BOOKED -> 
                ValidationResult.failure("Seat ${seat.seatNumber} is already booked")
            
            SeatStatus.BLOCKED -> 
                ValidationResult.failure("Seat ${seat.seatNumber} is temporarily blocked")
            
            SeatStatus.MAINTENANCE -> 
                ValidationResult.failure("Seat ${seat.seatNumber} is under maintenance")
        }
    }
}
