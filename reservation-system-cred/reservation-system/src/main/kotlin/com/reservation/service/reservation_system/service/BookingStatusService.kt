package com.reservation.service.reservation_system.service

import com.reservation.service.reservation_system.repository.entity.cassandra.SeatStatus
import com.reservation.service.reservation_system.store.TravelScheduleStore
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class BookingStatusService(
    private val travelScheduleStore: TravelScheduleStore
) {
    
    private val logger = LoggerFactory.getLogger(BookingStatusService::class.java)
    
    fun updateSeatStatusToAvailable(flightId: String, seatId: UUID): Boolean {
        logger.info("Updating seat status to AVAILABLE - FlightId: $flightId, SeatId: $seatId")
        
        return try {
            // Find all schedules for the flight
            val schedules = travelScheduleStore.findByFlightId(flightId)
            
            // Find the schedule that contains the seat
            val targetSchedule = schedules.firstOrNull { schedule ->
                schedule.schedule.any { scheduleItem ->
                    scheduleItem.seats.any { seatInfo -> seatInfo.seatId == seatId }
                }
            }
            
            if (targetSchedule == null) {
                logger.warn("No schedule found containing seat - FlightId: $flightId, SeatId: $seatId")
                return false
            }
            
            // Update the seat status to AVAILABLE
            val updatedSchedule = targetSchedule.copy(
                schedule = targetSchedule.schedule.map { scheduleItem ->
                    scheduleItem.copy(
                        seats = scheduleItem.seats.map { seatInfo ->
                            if (seatInfo.seatId == seatId) {
                                logger.debug("Updating seat ${seatInfo.seatId} from ${seatInfo.seatStatus} to AVAILABLE")
                                seatInfo.copy(seatStatus = SeatStatus.AVAILABLE)
                            } else {
                                seatInfo
                            }
                        }
                    )
                }
            )
            
            // Save the updated schedule
            val savedSchedule = travelScheduleStore.save(updatedSchedule)
            
            logger.info("Successfully updated seat status to AVAILABLE - FlightId: $flightId, SeatId: $seatId")
            true
            
        } catch (e: Exception) {
            logger.error("Failed to update seat status to AVAILABLE - FlightId: $flightId, SeatId: $seatId", e)
            false
        }
    }
    
    fun getSeatStatus(flightId: String, seatId: UUID): String? {
        logger.debug("Getting seat status - FlightId: $flightId, SeatId: $seatId")
        
        return try {
            val schedules = travelScheduleStore.findByFlightId(flightId)
            
            // Find the seat and return its status
            schedules.forEach { schedule ->
                schedule.schedule.forEach { scheduleItem ->
                    scheduleItem.seats.forEach { seatInfo ->
                        if (seatInfo.seatId == seatId) {
                            logger.debug("Found seat status: ${seatInfo.seatStatus} for SeatId: $seatId")
                            return seatInfo.seatStatus.name
                        }
                    }
                }
            }
            
            logger.debug("Seat not found - FlightId: $flightId, SeatId: $seatId")
            null
            
        } catch (e: Exception) {
            logger.error("Error getting seat status - FlightId: $flightId, SeatId: $seatId", e)
            null
        }
    }
    
    fun updateSeatStatus(flightId: String, seatId: UUID, newStatus: SeatStatus): Boolean {
        logger.info("Updating seat status - FlightId: $flightId, SeatId: $seatId, NewStatus: $newStatus")
        
        return try {
            val schedules = travelScheduleStore.findByFlightId(flightId)
            
            val targetSchedule = schedules.firstOrNull { schedule ->
                schedule.schedule.any { scheduleItem ->
                    scheduleItem.seats.any { seatInfo -> seatInfo.seatId == seatId }
                }
            }
            
            if (targetSchedule == null) {
                logger.warn("No schedule found containing seat - FlightId: $flightId, SeatId: $seatId")
                return false
            }
            
            val updatedSchedule = targetSchedule.copy(
                schedule = targetSchedule.schedule.map { scheduleItem ->
                    scheduleItem.copy(
                        seats = scheduleItem.seats.map { seatInfo ->
                            if (seatInfo.seatId == seatId) {
                                logger.debug("Updating seat ${seatInfo.seatId} from ${seatInfo.seatStatus} to $newStatus")
                                seatInfo.copy(seatStatus = newStatus)
                            } else {
                                seatInfo
                            }
                        }
                    )
                }
            )
            
            travelScheduleStore.save(updatedSchedule)
            logger.info("Successfully updated seat status - FlightId: $flightId, SeatId: $seatId, NewStatus: $newStatus")
            true
            
        } catch (e: Exception) {
            logger.error("Failed to update seat status - FlightId: $flightId, SeatId: $seatId, NewStatus: $newStatus", e)
            false
        }
    }
}
