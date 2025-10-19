package com.reservation.service.reservation_system.service

import com.reservation.service.reservation_system.dto.PaymentResponse
import com.reservation.service.reservation_system.repository.entity.Ticket

interface PaymentService {
    
    fun initiatePayment(ticket: Ticket): PaymentResponse
    
    fun verifyPayment(transactionId: String): PaymentResponse
    
    fun refundPayment(transactionId: String): PaymentResponse
}
