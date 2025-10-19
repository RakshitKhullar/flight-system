package com.reservation.service.reservation_system.client.impl

import com.reservation.service.reservation_system.client.PaymentService
import com.reservation.service.reservation_system.dto.PaymentRequest
import com.reservation.service.reservation_system.dto.PaymentResponse
import com.reservation.service.reservation_system.repository.entity.PaymentStatus
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.util.UUID

@Service
class PaymentServiceImpl : PaymentService {

    private val logger = LoggerFactory.getLogger(PaymentServiceImpl::class.java)

    override fun initiatePayment(paymentRequest: PaymentRequest): PaymentResponse {
        return try {
            logger.info("Initiating payment for booking: ${paymentRequest.bookingId}, amount: ${paymentRequest.amount}")

            // Generate transaction ID
            val transactionId = generateTransactionId()

            // Simulate payment gateway call
            // In real implementation, this would call actual payment gateway
            val paymentUrl = "https://payment-gateway.com/pay/$transactionId"

            PaymentResponse(
                transactionId = transactionId,
                status = PaymentStatus.PAYMENT_PENDING,
                amount = paymentRequest.amount,
                currency = paymentRequest.currency,
                paymentUrl = paymentUrl,
            )
        } catch (e: Exception) {
            logger.error("Payment initiation failed for booking: ${paymentRequest.bookingId}", e)
            PaymentResponse(
                transactionId = UUID.randomUUID(),
                status = PaymentStatus.PAYMENT_FAILED,
                amount = paymentRequest.amount,
                currency = paymentRequest.currency,
            )
        }
    }

    override fun verifyPayment(transactionId: String): PaymentResponse {
        // Simulate payment verification
        return PaymentResponse(
            transactionId = UUID.fromString(transactionId),
            status = PaymentStatus.PAYMENT_COMPLETED,
            amount = BigDecimal.ZERO,
            currency = "INR",
        )
    }

    override fun refundPayment(transactionId: String, amount: BigDecimal): PaymentResponse {
        // Simulate refund process
        return PaymentResponse(
            transactionId = UUID.fromString(transactionId),
            status = PaymentStatus.REFUND_COMPLETED,
            amount = amount,
            currency = "INR",
        )
    }

    private fun generateTransactionId(): UUID {
        return UUID.randomUUID()
    }
}