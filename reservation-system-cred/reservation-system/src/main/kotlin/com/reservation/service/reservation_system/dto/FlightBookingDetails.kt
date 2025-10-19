package com.reservation.service.reservation_system.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import java.util.UUID

@JsonIgnoreProperties(ignoreUnknown = true)
data class FlightBookingDetails(
    @JsonProperty("vehicleId")
    val vehicleId: UUID,

    @JsonProperty("flightStartTime")
    val flightStartTime: String,
    
    @JsonProperty("flightEndTime")
    val flightEndTime: String,

    @JsonProperty("seatId")
    val seatId: UUID,
    
    @JsonProperty("sourceCode")
    val sourceCode: String,
    
    @JsonProperty("destinationCode")
    val destinationCode: String,
    
    @JsonProperty("flightTime")
    val flightTime: String,
    
    @JsonProperty("discountsApplied")
    val discountsApplied: List<DiscountDetails> = emptyList()
) : BookingDetails {
    
    override fun getBookingType(): String = "FLIGHT"
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class DiscountDetails(
    @JsonProperty("couponCode")
    val couponCode: String? = null,
    
    @JsonProperty("coinsUsed")
    val coinsUsed: String? = null
)
