package com.reservation.service.reservation_system.service

import com.reservation.service.reservation_system.repository.entity.cassandra.TravelSchedule
import com.reservation.service.reservation_system.repository.entity.cassandra.ScheduleItem
import com.reservation.service.reservation_system.repository.entity.cassandra.SeatInfo
import com.reservation.service.reservation_system.repository.entity.cassandra.SeatStatus
import com.reservation.service.reservation_system.store.TravelScheduleStore
import org.springframework.stereotype.Service
import java.util.*

@Service
class FlightScheduleService(
    private val travelScheduleStore: TravelScheduleStore
) {

    fun createFlightSchedule(flightId: String, scheduleItems: List<ScheduleItem>): TravelSchedule {
        val travelSchedule = TravelSchedule(
            vehicleId = flightId,
            schedule = scheduleItems
        )
        return travelScheduleStore.save(travelSchedule)
    }

    fun getFlightSchedules(flightId: String): List<TravelSchedule> {
        return travelScheduleStore.findByFlightId(flightId)
    }

    fun updateSeatStatus(scheduleId: UUID, seatId: UUID, newStatus: SeatStatus): TravelSchedule? {
        val schedule = travelScheduleStore.findById(scheduleId) ?: return null
        
        val updatedSchedule = schedule.copy(
            schedule = schedule.schedule.map { scheduleItem ->
                scheduleItem.copy(
                    seats = scheduleItem.seats.map { seatInfo ->
                        if (seatInfo.seatId == seatId) {
                            seatInfo.copy(seatStatus = newStatus)
                        } else {
                            seatInfo
                        }
                    }
                )
            }
        )
        
        return travelScheduleStore.save(updatedSchedule)
    }
    
    fun updateSeatStatusByFlightId(flightId: String, seatId: UUID, newStatus: SeatStatus): TravelSchedule? {
        val schedules = travelScheduleStore.findByFlightId(flightId)
        
        // Find the schedule that contains the seat
        val targetSchedule = schedules.firstOrNull { schedule ->
            schedule.schedule.any { scheduleItem ->
                scheduleItem.seats.any { seatInfo -> seatInfo.seatId == seatId }
            }
        } ?: return null
        
        // Update the seat status
        val updatedSchedule = targetSchedule.copy(
            schedule = targetSchedule.schedule.map { scheduleItem ->
                scheduleItem.copy(
                    seats = scheduleItem.seats.map { seatInfo ->
                        if (seatInfo.seatId == seatId) {
                            seatInfo.copy(seatStatus = newStatus)
                        } else {
                            seatInfo
                        }
                    }
                )
            }
        )
        
        return travelScheduleStore.save(updatedSchedule)
    }

    fun confirmBlockedSeatByFlightId(flightId: String, seatId: UUID): TravelSchedule? {
        val schedules = travelScheduleStore.findByFlightId(flightId)
        
        // Find the schedule that contains the blocked seat
        val targetSchedule = schedules.firstOrNull { schedule ->
            schedule.schedule.any { scheduleItem ->
                scheduleItem.seats.any { seatInfo -> 
                    seatInfo.seatId == seatId && seatInfo.seatStatus == SeatStatus.BLOCKED 
                }
            }
        } ?: throw IllegalStateException("Seat not found or not in BLOCKED status")
        
        // Update seat status from BLOCKED to BOOKED
        val updatedSchedule = targetSchedule.copy(
            schedule = targetSchedule.schedule.map { scheduleItem ->
                scheduleItem.copy(
                    seats = scheduleItem.seats.map { seatInfo ->
                        if (seatInfo.seatId == seatId && seatInfo.seatStatus == SeatStatus.BLOCKED) {
                            seatInfo.copy(seatStatus = SeatStatus.BOOKED)
                        } else {
                            seatInfo
                        }
                    }
                )
            }
        )
        
        return travelScheduleStore.save(updatedSchedule)
    }
    
    fun getAvailableSeats(flightId: String, date: String?): List<SeatInfo> {
        return travelScheduleStore.findByFlightId(flightId)
            .flatMap { travelSchedule ->
                travelSchedule.schedule
                    .flatMap { scheduleItem -> scheduleItem.seats }
                    .filter { seatInfo -> seatInfo.seatStatus == SeatStatus.AVAILABLE }
            }
    }

    fun getBlockedSeats(flightId: String): List<SeatInfo> {
        return travelScheduleStore.findByFlightId(flightId)
            .flatMap { travelSchedule ->
                travelSchedule.schedule
                    .flatMap { scheduleItem -> scheduleItem.seats }
                    .filter { seatInfo -> seatInfo.seatStatus == SeatStatus.BLOCKED }
            }
    }

    fun getAllFlightSchedules(): List<TravelSchedule> {
        return travelScheduleStore.findAll()
    }

    fun deleteFlightSchedule(id: UUID): Boolean {
        return if (travelScheduleStore.existsById(id)) {
            travelScheduleStore.deleteById(id)
            true
        } else {
            false
        }
    }
}
