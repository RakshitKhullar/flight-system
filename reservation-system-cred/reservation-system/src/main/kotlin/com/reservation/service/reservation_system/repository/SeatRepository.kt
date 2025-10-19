package com.reservation.service.reservation_system.repository

import com.reservation.service.reservation_system.repository.entity.Seat
import com.reservation.service.reservation_system.repository.entity.Vehicle
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface SeatRepository : JpaRepository<Seat, Long> {
    
    fun findAllByVehicle(vehicle: Vehicle): List<Seat>
    
    fun findAllByVehicleAndIsAvailable(vehicle: Vehicle, isAvailable: Boolean): List<Seat>
    
    fun findAllByVehicleAndSeatType(vehicle: Vehicle, seatType: Seat.SeatType): List<Seat>
    
    fun findAllByVehicleAndIsWindowSeat(vehicle: Vehicle, isWindowSeat: Boolean): List<Seat>
    
    fun findBySeatNumberAndVehicle(seatNumber: String, vehicle: Vehicle): Seat?
    
    @Query("SELECT s FROM Seat s WHERE s.vehicle.vehicleId = :vehicleId")
    fun findAllByVehicleId(@Param("vehicleId") vehicleId: UUID): List<Seat>
    
    @Query("SELECT s FROM Seat s WHERE s.vehicle.vehicleId = :vehicleId AND s.isAvailable = :isAvailable")
    fun findAllByVehicleIdAndAvailability(
        @Param("vehicleId") vehicleId: UUID, 
        @Param("isAvailable") isAvailable: Boolean
    ): List<Seat>
    
    fun countByVehicleAndIsAvailable(vehicle: Vehicle, isAvailable: Boolean): Long
}
