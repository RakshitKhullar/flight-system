package com.reservation.service.reservation_system.dto

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.util.UUID

data class BookingRequest(
    val userId: UUID,
    val bookingType: BookingType,
    @JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
        property = "bookingType"
    )
    @JsonSubTypes(
        JsonSubTypes.Type(value = FlightBookingDetails::class, name = "FLIGHT")
    )
    val bookingDetails: BookingDetails
)

enum class BookingType {
    FLIGHT,
    HOTEL,
    CAR_RENTAL
}
