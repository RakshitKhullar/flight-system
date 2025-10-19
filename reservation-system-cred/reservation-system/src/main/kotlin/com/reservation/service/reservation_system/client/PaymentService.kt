package com.reservation.service.reservation_system.client

import com.reservation.service.reservation_system.dto.PaymentRequest
import com.reservation.service.reservation_system.dto.PaymentResponse
import java.math.BigDecimal

interface PaymentService {
    fun initiatePayment(paymentRequest: PaymentRequest): PaymentResponse
    fun verifyPayment(transactionId: String): PaymentResponse
    fun refundPayment(transactionId: String, amount: BigDecimal): PaymentResponse
}