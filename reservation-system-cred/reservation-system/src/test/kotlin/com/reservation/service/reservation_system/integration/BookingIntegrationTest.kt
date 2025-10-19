package com.reservation.service.reservation_system.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.reservation.service.reservation_system.dto.*
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.transaction.annotation.Transactional
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors

@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
class BookingIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `should handle complete booking flow end to end`() {
        // Given
        val userId = UUID.randomUUID()
        val seatId = UUID.randomUUID()
        val vehicleId = UUID.randomUUID()

        val flightDetails = FlightBookingDetails(
            vehicleId = vehicleId,
            flightStartTime = "10:00",
            flightEndTime = "12:00",
            seatId = seatId,
            sourceCode = "DEL",
            destinationCode = "BOM",
            flightTime = "10:30",
            discountsApplied = emptyList()
        )

        val bookingRequest = BookingRequest(
            userId = userId,
            bookingType = BookingType.FLIGHT,
            bookingDetails = flightDetails
        )

        // When & Then
        mockMvc.perform(
            post("/api/book-tickets")
                .header("user-id", userId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(bookingRequest))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.userId").value(userId.toString()))
            .andExpect(jsonPath("$.bookingType").value("FLIGHT"))
            .andExpect(jsonPath("$.bookingStatus").exists())
            .andExpect(jsonPath("$.paymentStatus").exists())
            .andExpect(jsonPath("$.pnrNumber").exists())
            .andExpect(jsonPath("$.totalAmount").exists())
    }

    @Test
    fun `should handle concurrent booking requests for same seat`() {
        // Given
        val userId1 = UUID.randomUUID()
        val userId2 = UUID.randomUUID()
        val seatId = UUID.randomUUID()
        val vehicleId = UUID.randomUUID()

        val bookingRequest1 = createBookingRequest(userId1, seatId, vehicleId)
        val bookingRequest2 = createBookingRequest(userId2, seatId, vehicleId)

        // When - simulate concurrent requests
        val result1 = mockMvc.perform(
            post("/api/book-tickets")
                .header("user-id", userId1.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(bookingRequest1))
        )

        val result2 = mockMvc.perform(
            post("/api/book-tickets")
                .header("user-id", userId2.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(bookingRequest2))
        )

        // Then - one should succeed, other should get conflict
        val results = listOf(result1, result2)
        val successCount = results.count { 
            it.andReturn().response.status == 201 
        }
        val conflictCount = results.count { 
            it.andReturn().response.status == 409 
        }

        // At least one should succeed, others should get conflict
        assert(successCount >= 1)
        assert(conflictCount >= 0)
        assert(successCount + conflictCount == 2)
    }

    @Test
    fun `should handle duplicate booking requests from same user - only one should succeed`() {
        // Given - Same user making duplicate requests for the same seat
        val userId = UUID.randomUUID()
        val seatId = UUID.randomUUID()
        val vehicleId = UUID.randomUUID()

        val bookingRequest = createBookingRequest(userId, seatId, vehicleId)
        val requestJson = objectMapper.writeValueAsString(bookingRequest)

        // When - Execute two identical requests concurrently using CompletableFuture
        val executor = Executors.newFixedThreadPool(2)
        
        val future1 = CompletableFuture.supplyAsync({
            mockMvc.perform(
                post("/api/book-tickets")
                    .header("user-id", userId.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson)
            ).andReturn()
        }, executor)

        val future2 = CompletableFuture.supplyAsync({
            mockMvc.perform(
                post("/api/book-tickets")
                    .header("user-id", userId.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson)
            ).andReturn()
        }, executor)

        // Wait for both requests to complete
        val result1 = future1.get()
        val result2 = future2.get()
        
        executor.shutdown()

        // Then - Verify results
        val responses = listOf(result1, result2)
        val successResponses = responses.filter { it.response.status == 201 }
        val conflictResponses = responses.filter { it.response.status == 409 }

        // Assertions
        assert(successResponses.size == 1) { 
            "Expected exactly 1 successful booking, but got ${successResponses.size}" 
        }
        assert(conflictResponses.size == 1) { 
            "Expected exactly 1 conflict response, but got ${conflictResponses.size}" 
        }

        // Verify the conflict response contains expected error details
        val conflictResponse = conflictResponses.first()
        val conflictBody = objectMapper.readTree(conflictResponse.response.contentAsString)
        
        assert(conflictBody.has("error")) { "Conflict response should contain 'error' field" }
        assert(conflictBody.get("error").asText() == "SEAT_BOOKING_IN_PROGRESS") { 
            "Expected error type 'SEAT_BOOKING_IN_PROGRESS'" 
        }
        assert(conflictBody.has("message")) { "Conflict response should contain 'message' field" }
        assert(conflictBody.has("timestamp")) { "Conflict response should contain 'timestamp' field" }

        // Verify the successful response contains ticket details
        val successResponse = successResponses.first()
        val successBody = objectMapper.readTree(successResponse.response.contentAsString)
        
        assert(successBody.has("userId")) { "Success response should contain 'userId' field" }
        assert(successBody.get("userId").asText() == userId.toString()) { 
            "Success response userId should match request userId" 
        }
        assert(successBody.has("bookingType")) { "Success response should contain 'bookingType' field" }
        assert(successBody.has("pnrNumber")) { "Success response should contain 'pnrNumber' field" }
    }

    @Test
    fun `should handle rapid duplicate requests using coroutines`() = runBlocking {
        // Given
        val userId = UUID.randomUUID()
        val seatId = UUID.randomUUID()
        val vehicleId = UUID.randomUUID()

        val bookingRequest = createBookingRequest(userId, seatId, vehicleId)
        val requestJson = objectMapper.writeValueAsString(bookingRequest)

        // When - Execute requests using coroutines for true concurrency
        val deferred1 = async {
            mockMvc.perform(
                post("/api/book-tickets")
                    .header("user-id", userId.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson)
            ).andReturn()
        }

        val deferred2 = async {
            mockMvc.perform(
                post("/api/book-tickets")
                    .header("user-id", userId.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson)
            ).andReturn()
        }

        // Await both results
        val result1 = deferred1.await()
        val result2 = deferred2.await()

        // Then - Verify only one succeeds
        val statuses = listOf(result1.response.status, result2.response.status)
        val successCount = statuses.count { it == 201 }
        val conflictCount = statuses.count { it == 409 }

        assert(successCount == 1) { "Expected exactly 1 success, got $successCount" }
        assert(conflictCount == 1) { "Expected exactly 1 conflict, got $conflictCount" }
    }

    private fun createBookingRequest(userId: UUID, seatId: UUID, vehicleId: UUID): BookingRequest {
        val flightDetails = FlightBookingDetails(
            vehicleId = vehicleId,
            flightStartTime = "10:00",
            flightEndTime = "12:00",
            seatId = seatId,
            sourceCode = "DEL",
            destinationCode = "BOM",
            flightTime = "10:30",
            discountsApplied = emptyList()
        )

        return BookingRequest(
            userId = userId,
            bookingType = BookingType.FLIGHT,
            bookingDetails = flightDetails
        )
    }
}
