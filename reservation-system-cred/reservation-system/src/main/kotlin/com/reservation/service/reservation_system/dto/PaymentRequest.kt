package com.reservation.service.reservation_system.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal
import java.util.UUID

@JsonIgnoreProperties(ignoreUnknown = true)
data class PaymentRequest(
    @JsonProperty("userId")
    val userId: UUID,
    
    @JsonProperty("amount")
    val amount: BigDecimal,
    
    @JsonProperty("currency")
    val currency: String = "INR",
    
    @JsonProperty("bookingId")
    val bookingId: UUID,
    
    @JsonProperty("seatId")
    val seatId: UUID,
    
    @JsonProperty("description")
    val description: String,
    
    @JsonProperty("paymentMethod")
    val paymentMethod: String = "UPI"
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class PaymentResponse(
    @JsonProperty("transactionId")
    val transactionId: UUID,
    
    @JsonProperty("status")
    val status: com.reservation.service.reservation_system.repository.entity.PaymentStatus,
    
    @JsonProperty("amount")
    val amount: BigDecimal,
    
    @JsonProperty("currency")
    val currency: String,
    
    @JsonProperty("paymentUrl")
    val paymentUrl: String? = null,

)
