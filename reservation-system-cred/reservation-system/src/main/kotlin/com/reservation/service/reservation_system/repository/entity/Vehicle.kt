package com.reservation.service.reservation_system.repository.entity

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import org.hibernate.annotations.UuidGenerator
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "vehicles")
data class Vehicle(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "vehicle_id", nullable = false, unique = true)
    @UuidGenerator
    val vehicleId: UUID = UUID.randomUUID(),

    @Column(name = "is_available", nullable = false)
    var isAvailable: Boolean = true,

    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_type", nullable = false)
    val vehicleType: VehicleType,

    @Enumerated(EnumType.STRING)
    @Column(name = "owner_type", nullable = false)
    val ownerType: OwnerType,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @UpdateTimestamp
    @Column(name = "updated_at")
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    enum class VehicleType {
        FLIGHT,
        BUS,
        TRAIN,
        CAR,
        BOAT
    }

    enum class OwnerType {
        // Airlines
        INDIGO,
        AIR_INDIA,
        SPICEJET,
        VISTARA,
        GO_FIRST,
        AKASA_AIR,
        ALLIANCE_AIR,
        
        // International Airlines
        EMIRATES,
        QATAR_AIRWAYS,
        SINGAPORE_AIRLINES,
        LUFTHANSA,
        BRITISH_AIRWAYS,
        
        // Bus Operators
        REDBUS,
        APSRTC,
        KSRTC,
        MSRTC,
        UPSRTC,
        VOLVO,
        
        // Railways
        INDIAN_RAILWAYS,
        METRO_RAIL,
        
        // Car Rentals
        OLA,
        UBER,
        ZOOMCAR,
        DRIVEZY,
        
        // Others
        PRIVATE,
        GOVERNMENT,
        OTHER
    }
}
