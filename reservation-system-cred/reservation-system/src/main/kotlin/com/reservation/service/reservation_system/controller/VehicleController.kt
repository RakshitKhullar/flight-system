package com.reservation.service.reservation_system.controller

import com.reservation.service.reservation_system.repository.entity.Vehicle
import com.reservation.service.reservation_system.service.VehicleService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/vehicles")
class VehicleController(
    private val vehicleService: VehicleService
) {

    @PostMapping
    fun createVehicle(
        @RequestParam vehicleType: Vehicle.VehicleType,
        @RequestParam ownerType: Vehicle.OwnerType
    ): ResponseEntity<Vehicle> {
        val vehicle = vehicleService.createVehicle(vehicleType, ownerType)
        return ResponseEntity(vehicle, HttpStatus.CREATED)
    }

    @GetMapping
    fun getAllVehicles(): ResponseEntity<List<Vehicle>> {
        val vehicles = vehicleService.getAllVehicles()
        return ResponseEntity.ok(vehicles)
    }

    @GetMapping("/{vehicleId}")
    fun getVehicleById(@PathVariable vehicleId: UUID): ResponseEntity<Vehicle> {
        return vehicleService.getVehicleById(vehicleId)
            ?.let { ResponseEntity.ok(it) }
            ?: ResponseEntity.notFound().build()
    }

    @GetMapping("/type/{vehicleType}")
    fun getVehiclesByType(@PathVariable vehicleType: Vehicle.VehicleType): ResponseEntity<List<Vehicle>> {
        val vehicles = vehicleService.getVehiclesByType(vehicleType)
        return ResponseEntity.ok(vehicles)
    }

    @GetMapping("/available")
    fun getAvailableVehicles(): ResponseEntity<List<Vehicle>> {
        val vehicles = vehicleService.getAvailableVehicles()
        return ResponseEntity.ok(vehicles)
    }

    @GetMapping("/available/type/{vehicleType}")
    fun getAvailableVehiclesByType(@PathVariable vehicleType: Vehicle.VehicleType): ResponseEntity<List<Vehicle>> {
        val vehicles = vehicleService.getAvailableVehiclesByType(vehicleType)
        return ResponseEntity.ok(vehicles)
    }

    @PutMapping("/{vehicleId}/availability")
    fun updateVehicleAvailability(
        @PathVariable vehicleId: UUID,
        @RequestParam isAvailable: Boolean
    ): ResponseEntity<Vehicle> {
        return vehicleService.updateVehicleAvailability(vehicleId, isAvailable)
            ?.let { ResponseEntity.ok(it) }
            ?: ResponseEntity.notFound().build()
    }

    @DeleteMapping("/{vehicleId}")
    fun deleteVehicle(@PathVariable vehicleId: UUID): ResponseEntity<Void> {
        return if (vehicleService.deleteVehicle(vehicleId)) {
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }
}
