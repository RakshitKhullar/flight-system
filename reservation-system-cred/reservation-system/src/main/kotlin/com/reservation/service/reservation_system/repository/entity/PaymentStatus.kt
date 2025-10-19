package com.reservation.service.reservation_system.repository.entity

enum class PaymentStatus {
    PAYMENT_PENDING,
    PAYMENT_COMPLETED,
    PAYMENT_FAILED,
    REFUND_INITIATED,
    REFUND_COMPLETED
}
