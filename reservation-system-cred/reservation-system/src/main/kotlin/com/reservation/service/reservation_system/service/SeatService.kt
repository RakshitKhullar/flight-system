package com.reservation.service.reservation_system.service

import com.reservation.service.reservation_system.repository.SeatRepository
import com.reservation.service.reservation_system.repository.VehicleRepository
import com.reservation.service.reservation_system.repository.entity.Seat
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class SeatService(
    private val seatRepository: SeatRepository,
    private val vehicleRepository: VehicleRepository
) {

    fun createSeat(
        vehicleId: UUID,
        seatNumber: String,
        seatType: Seat.SeatType,
        isWindowSeat: Boolean = false
    ): Seat? {
        val vehicle = vehicleRepository.findByVehicleId(vehicleId) ?: return null
        
        val seat = Seat(
            seatNumber = seatNumber,
            seatType = seatType,
            isWindowSeat = isWindowSeat,
            vehicle = vehicle,
            isAvailable = true
        )
        return seatRepository.save(seat)
    }

    fun getSeatsByVehicle(vehicleId: UUID): List<Seat> {
        return seatRepository.findAllByVehicleId(vehicleId)
    }

    fun getAvailableSeatsByVehicle(vehicleId: UUID): List<Seat> {
        return seatRepository.findAllByVehicleIdAndAvailability(vehicleId, true)
    }

    fun getSeatByNumberAndVehicle(seatNumber: String, vehicleId: UUID): Seat? {
        val vehicle = vehicleRepository.findByVehicleId(vehicleId) ?: return null
        return seatRepository.findBySeatNumberAndVehicle(seatNumber, vehicle)
    }

    fun updateSeatAvailability(seatId: Long, isAvailable: Boolean): Seat? {
        val seat = seatRepository.findById(seatId).orElse(null) ?: return null
        val updatedSeat = seat.copy(isAvailable = isAvailable)
        return seatRepository.save(updatedSeat)
    }

    fun getWindowSeatsByVehicle(vehicleId: UUID): List<Seat> {
        val vehicle = vehicleRepository.findByVehicleId(vehicleId) ?: return emptyList()
        return seatRepository.findAllByVehicleAndIsWindowSeat(vehicle, true)
    }

    fun getSeatsByTypeAndVehicle(vehicleId: UUID, seatType: Seat.SeatType): List<Seat> {
        val vehicle = vehicleRepository.findByVehicleId(vehicleId) ?: return emptyList()
        return seatRepository.findAllByVehicleAndSeatType(vehicle, seatType)
    }

    fun getAvailableSeatCount(vehicleId: UUID): Long {
        val vehicle = vehicleRepository.findByVehicleId(vehicleId) ?: return 0
        return seatRepository.countByVehicleAndIsAvailable(vehicle, true)
    }

    fun deleteSeat(seatId: Long): Boolean {
        return if (seatRepository.existsById(seatId)) {
            seatRepository.deleteById(seatId)
            true
        } else {
            false
        }
    }
}
