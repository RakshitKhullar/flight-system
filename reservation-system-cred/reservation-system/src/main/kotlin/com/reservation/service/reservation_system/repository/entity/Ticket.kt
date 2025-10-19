package com.reservation.service.reservation_system.repository.entity

import com.reservation.service.reservation_system.dto.BookingType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import org.hibernate.annotations.UuidGenerator
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "tickets")
data class Ticket(
    @Id
    @Column(name = "ticket_id", nullable = false)
    @UuidGenerator
    val ticketId: UUID = UUID.randomUUID(),

    @Column(name = "user_id", nullable = false)
    val userId: UUID,

    @Column(name = "pnr_number", nullable = false, unique = true)
    val pnrNumber: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "booking_type", nullable = false)
    val bookingType: BookingType,

    @Enumerated(EnumType.STRING)
    @Column(name = "booking_status", nullable = false)
    val bookingStatus: BookingStatus,

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    val paymentStatus: PaymentStatus,

    @Column(name = "flight_id", nullable = false)
    val flightId: String,

    @Column(name = "seat_id", nullable = false)
    val seatId: UUID,

    @Column(name = "total_amount", nullable = false)
    val totalAmount: BigDecimal,

    @Column(name = "booking_details", columnDefinition = "TEXT")
    val bookingDetails: String? = null,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @UpdateTimestamp
    @Column(name = "updated_at")
    val updatedAt: LocalDateTime = LocalDateTime.now()
)