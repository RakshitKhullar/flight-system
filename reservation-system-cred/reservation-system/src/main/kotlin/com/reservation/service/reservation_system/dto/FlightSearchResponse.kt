package com.reservation.service.reservation_system.dto

import com.fasterxml.jackson.annotation.JsonProperty
import com.reservation.service.reservation_system.repository.entity.cassandra.FlightType
import com.reservation.service.reservation_system.repository.entity.cassandra.SeatClass
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

data class FlightSearchResponse(
    @JsonProperty("scheduleId")
    val scheduleId: UUID,
    
    @JsonProperty("flightNumber")
    val flightNumber: String,
    
    @JsonProperty("date")
    val date: LocalDate,
    
    @JsonProperty("sourceCode")
    val sourceCode: String,
    
    @JsonProperty("destinationCode")
    val destinationCode: String,
    
    @JsonProperty("travelStartTime")
    val travelStartTime: LocalTime,
    
    @JsonProperty("travelEndTime")
    val travelEndTime: LocalTime,
    
    @JsonProperty("numberOfStops")
    val numberOfStops: Int,
    
    @JsonProperty("isDirect")
    val isDirect: Boolean,
    
    @JsonProperty("flightType")
    val flightType: FlightType,
    
    @JsonProperty("stops")
    val stops: List<FlightStopResponse>,
    
    @JsonProperty("totalSeats")
    val totalSeats: Int,
    
    @JsonProperty("availableSeats")
    val availableSeats: Int,
    
    @JsonProperty("minPrice")
    val minPrice: BigDecimal,
    
    @JsonProperty("maxPrice")
    val maxPrice: BigDecimal,
    
    @JsonProperty("availableSeatClasses")
    val availableSeatClasses: List<SeatClass>,
    
    @JsonProperty("totalTravelTime")
    val totalTravelTime: Int, // in minutes
    
    @JsonProperty("totalLayoverTime")
    val totalLayoverTime: Int // in minutes
)

data class FlightDetailsResponse(
    @JsonProperty("scheduleId")
    val scheduleId: UUID,
    
    @JsonProperty("flightNumber")
    val flightNumber: String,
    
    @JsonProperty("date")
    val date: LocalDate,
    
    @JsonProperty("sourceCode")
    val sourceCode: String,
    
    @JsonProperty("destinationCode")
    val destinationCode: String,
    
    @JsonProperty("travelStartTime")
    val travelStartTime: LocalTime,
    
    @JsonProperty("travelEndTime")
    val travelEndTime: LocalTime,
    
    @JsonProperty("numberOfStops")
    val numberOfStops: Int,
    
    @JsonProperty("isDirect")
    val isDirect: Boolean,
    
    @JsonProperty("flightType")
    val flightType: FlightType,
    
    @JsonProperty("stops")
    val stops: List<FlightStopResponse>,
    
    @JsonProperty("seats")
    val seats: List<SeatDetailsResponse>,
    
    @JsonProperty("totalSeats")
    val totalSeats: Int,
    
    @JsonProperty("availableSeats")
    val availableSeats: Int,
    
    @JsonProperty("totalTravelTime")
    val totalTravelTime: Int,
    
    @JsonProperty("totalLayoverTime")
    val totalLayoverTime: Int
)

data class SeatDetailsResponse(
    @JsonProperty("seatId")
    val seatId: UUID,
    
    @JsonProperty("seatNumber")
    val seatNumber: String,
    
    @JsonProperty("seatClass")
    val seatClass: SeatClass,
    
    @JsonProperty("amount")
    val amount: BigDecimal,
    
    @JsonProperty("seatStatus")
    val seatStatus: String,
    
    @JsonProperty("isAvailable")
    val isAvailable: Boolean
)

data class SeatStatusUpdateRequest(
    @JsonProperty("status")
    val status: String,
    
    @JsonProperty("reason")
    val reason: String? = null
)
