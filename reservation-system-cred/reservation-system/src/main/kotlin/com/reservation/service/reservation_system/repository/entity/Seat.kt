package com.reservation.service.reservation_system.repository.entity

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDateTime

@Entity
@Table(name = "seats")
data class Seat(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "seat_number", nullable = false)
    val seatNumber: String,

    @Column(name = "is_window_seat", nullable = false)
    val isWindowSeat: Boolean = false,

    @Column(name = "is_available", nullable = false)
    var isAvailable: Boolean = true,

    @Enumerated(EnumType.STRING)
    @Column(name = "seat_type", nullable = false)
    val seatType: SeatType,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    val vehicle: Vehicle,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @UpdateTimestamp
    @Column(name = "updated_at")
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    enum class SeatType {
        ECONOMY,
        PREMIUM_ECONOMY,
        BUSINESS,
        FIRST_CLASS,
        SLEEPER,
        GENERAL,
        AC,
        NON_AC
    }
}
