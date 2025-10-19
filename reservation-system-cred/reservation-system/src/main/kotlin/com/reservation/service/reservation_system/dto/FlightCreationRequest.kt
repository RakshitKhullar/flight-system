package com.reservation.service.reservation_system.dto

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
import com.reservation.service.reservation_system.repository.entity.cassandra.SeatClass
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

data class FlightCreationRequest(
    @JsonProperty("flightNumber")
    val flightNumber: String,
    
    @JsonProperty("date")
    @JsonFormat(pattern = "yyyy-MM-dd")
    val date: LocalDate,
    
    @JsonProperty("sourceCode")
    val sourceCode: String,
    
    @JsonProperty("destinationCode")
    val destinationCode: String,
    
    @JsonProperty("travelStartTime")
    @JsonFormat(pattern = "HH:mm")
    val travelStartTime: LocalTime,
    
    @JsonProperty("travelEndTime")
    @JsonFormat(pattern = "HH:mm")
    val travelEndTime: LocalTime,
    
    @JsonProperty("numberOfStops")
    val numberOfStops: Int = 0,
    
    @JsonProperty("stops")
    val stops: List<FlightStopRequest> = emptyList(),
    
    @JsonProperty("seats")
    val seats: List<SeatCreationRequest> = emptyList(),
    
    @JsonProperty("aircraftType")
    val aircraftType: String? = null,
    
    @JsonProperty("airline")
    val airline: String? = null
)

data class FlightStopRequest(
    @JsonProperty("airportCode")
    val airportCode: String,
    
    @JsonProperty("airportName")
    val airportName: String,
    
    @JsonProperty("city")
    val city: String,
    
    @JsonProperty("arrivalTime")
    @JsonFormat(pattern = "HH:mm")
    val arrivalTime: LocalTime,
    
    @JsonProperty("departureTime")
    @JsonFormat(pattern = "HH:mm")
    val departureTime: LocalTime,
    
    @JsonProperty("layoverDuration")
    val layoverDuration: Int, // in minutes
    
    @JsonProperty("stopSequence")
    val stopSequence: Int
)

data class SeatCreationRequest(
    @JsonProperty("seatNumber")
    val seatNumber: String,
    
    @JsonProperty("seatClass")
    val seatClass: SeatClass,
    
    @JsonProperty("amount")
    val amount: BigDecimal
)

data class FlightCreationResponse(
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
    
    @JsonProperty("numberOfStops")
    val numberOfStops: Int,
    
    @JsonProperty("isDirect")
    val isDirect: Boolean,
    
    @JsonProperty("totalSeats")
    val totalSeats: Int,
    
    @JsonProperty("availableSeats")
    val availableSeats: Int,
    
    @JsonProperty("stops")
    val stops: List<FlightStopResponse>,
    
    @JsonProperty("message")
    val message: String,
    
    @JsonProperty("createdAt")
    val createdAt: Long = System.currentTimeMillis()
)

data class FlightStopResponse(
    @JsonProperty("stopId")
    val stopId: UUID,
    
    @JsonProperty("airportCode")
    val airportCode: String,
    
    @JsonProperty("airportName")
    val airportName: String,
    
    @JsonProperty("city")
    val city: String,
    
    @JsonProperty("arrivalTime")
    val arrivalTime: LocalTime,
    
    @JsonProperty("departureTime")
    val departureTime: LocalTime,
    
    @JsonProperty("layoverDuration")
    val layoverDuration: Int,
    
    @JsonProperty("stopSequence")
    val stopSequence: Int
)
