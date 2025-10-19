package com.reservation.service.reservation_system.repository

import com.reservation.service.reservation_system.repository.entity.Vehicle
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface VehicleRepository : JpaRepository<Vehicle, Long> {
    
    fun findByVehicleId(vehicleId: UUID): Vehicle?
    
    fun findAllByVehicleType(vehicleType: Vehicle.VehicleType): List<Vehicle>
    
    fun findAllByOwnerType(ownerType: Vehicle.OwnerType): List<Vehicle>
    
    fun findAllByIsAvailable(isAvailable: Boolean): List<Vehicle>
    
    fun findAllByVehicleTypeAndIsAvailable(
        vehicleType: Vehicle.VehicleType, 
        isAvailable: Boolean
    ): List<Vehicle>
    
    fun existsByVehicleId(vehicleId: UUID): Boolean
}
