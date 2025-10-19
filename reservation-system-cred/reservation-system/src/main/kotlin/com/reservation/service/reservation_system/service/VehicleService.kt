package com.reservation.service.reservation_system.service

import com.reservation.service.reservation_system.repository.VehicleRepository
import com.reservation.service.reservation_system.repository.entity.Vehicle
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class VehicleService(
    private val vehicleRepository: VehicleRepository
) {

    fun createVehicle(vehicleType: Vehicle.VehicleType, ownerType: Vehicle.OwnerType): Vehicle {
        val vehicle = Vehicle(
            vehicleType = vehicleType,
            ownerType = ownerType,
            isAvailable = true
        )
        return vehicleRepository.save(vehicle)
    }

    fun getVehicleById(vehicleId: UUID): Vehicle? {
        return vehicleRepository.findByVehicleId(vehicleId)
    }

    fun getAllVehicles(): List<Vehicle> {
        return vehicleRepository.findAll()
    }

    fun getVehiclesByType(vehicleType: Vehicle.VehicleType): List<Vehicle> {
        return vehicleRepository.findAllByVehicleType(vehicleType)
    }

    fun getAvailableVehicles(): List<Vehicle> {
        return vehicleRepository.findAllByIsAvailable(true)
    }

    fun getAvailableVehiclesByType(vehicleType: Vehicle.VehicleType): List<Vehicle> {
        return vehicleRepository.findAllByVehicleTypeAndIsAvailable(vehicleType, true)
    }

    fun updateVehicleAvailability(vehicleId: UUID, isAvailable: Boolean): Vehicle? {
        val vehicle = vehicleRepository.findByVehicleId(vehicleId) ?: return null
        val updatedVehicle = vehicle.copy(isAvailable = isAvailable)
        return vehicleRepository.save(updatedVehicle)
    }

    fun deleteVehicle(vehicleId: UUID): Boolean {
        val vehicle = vehicleRepository.findByVehicleId(vehicleId) ?: return false
        vehicleRepository.delete(vehicle)
        return true
    }
}
