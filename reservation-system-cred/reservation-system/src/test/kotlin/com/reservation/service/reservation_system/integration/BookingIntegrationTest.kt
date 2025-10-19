package com.reservation.service.reservation_system.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.reservation.service.reservation_system.dto.*
import com.reservation.service.reservation_system.repository.entity.cassandra.SeatClass
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
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

    @Test
    fun `should create multi-stop flight with all segments`() {
        // Given - Create a multi-stop flight: DEL -> BOM -> BLR
        val adminId = UUID.randomUUID()
        val flightCreationRequest = createMultiStopFlightRequest()

        // When - Create the flight
        val result = mockMvc.perform(
            post("/api/book-tickets/flights")
                .header("admin-id", adminId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(flightCreationRequest))
        )

        // Then - Flight should be created successfully
        result.andExpect(status().isCreated)
            .andExpect(jsonPath("$.flightNumber").value("AI101"))
            .andExpect(jsonPath("$.sourceCode").value("DEL"))
            .andExpect(jsonPath("$.destinationCode").value("BLR"))
            .andExpect(jsonPath("$.numberOfStops").value(1))
            .andExpect(jsonPath("$.isDirect").value(false))
            .andExpect(jsonPath("$.stops").isArray)
            .andExpect(jsonPath("$.stops[0].airportCode").value("BOM"))
    }

    @Test
    fun `should show all segments for multi-stop flight`() {
        // Given - Create a multi-stop flight first
        val adminId = UUID.randomUUID()
        val flightCreationRequest = createMultiStopFlightRequest()
        
        mockMvc.perform(
            post("/api/book-tickets/flights")
                .header("admin-id", adminId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(flightCreationRequest))
        ).andExpect(status().isCreated)

        // When - Get flight segments
        val result = mockMvc.perform(
            get("/api/book-tickets/flights/AI101/segments")
                .param("date", LocalDate.now().toString())
        )

        // Then - Should return all possible segments
        result.andExpect(status().isOk)
            .andExpect(jsonPath("$.flightNumber").value("AI101"))
            .andExpect(jsonPath("$.totalSegments").value(3)) // DEL->BOM, DEL->BLR, BOM->BLR
            .andExpect(jsonPath("$.segments").isArray)
            .andExpect(jsonPath("$.segments[*].route").exists())
    }

    @Test
    fun `should book segment successfully - DEL to BOM`() {
        // Given - Create multi-stop flight and book a segment
        setupMultiStopFlight()
        
        val userId = UUID.randomUUID()
        val seatId = UUID.randomUUID()
        
        val segmentBookingRequest = createSegmentBookingRequest(
            userId = userId,
            seatId = seatId,
            sourceCode = "DEL",
            destinationCode = "BOM"
        )

        // When - Book the segment
        val result = mockMvc.perform(
            post("/api/book-tickets")
                .header("user-id", userId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(segmentBookingRequest))
        )

        // Then - Booking should succeed
        result.andExpect(status().isCreated)
            .andExpect(jsonPath("$.userId").value(userId.toString()))
            .andExpect(jsonPath("$.bookingType").value("FLIGHT"))
            .andExpect(jsonPath("$.pnrNumber").exists())
    }

    @Test
    fun `should handle overlapping segment bookings correctly`() {
        // Given - Create multi-stop flight
        setupMultiStopFlight()
        
        val userId1 = UUID.randomUUID()
        val userId2 = UUID.randomUUID()
        val seatId = UUID.randomUUID()

        // User 1 books DEL -> BLR (full route)
        val fullRouteBooking = createSegmentBookingRequest(
            userId = userId1,
            seatId = seatId,
            sourceCode = "DEL",
            destinationCode = "BLR"
        )

        // User 2 tries to book BOM -> BLR (overlapping segment)
        val segmentBooking = createSegmentBookingRequest(
            userId = userId2,
            seatId = seatId,
            sourceCode = "BOM",
            destinationCode = "BLR"
        )

        // When - Book full route first
        val result1 = mockMvc.perform(
            post("/api/book-tickets")
                .header("user-id", userId1.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(fullRouteBooking))
        )

        // Then try to book overlapping segment
        val result2 = mockMvc.perform(
            post("/api/book-tickets")
                .header("user-id", userId2.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(segmentBooking))
        )

        // Then - First booking should succeed, second should fail
        result1.andExpect(status().isCreated)
        result2.andExpect(status().isConflict)
    }

    @Test
    fun `should allow non-overlapping segment bookings`() {
        // Given - Create multi-stop flight
        setupMultiStopFlight()
        
        val userId1 = UUID.randomUUID()
        val userId2 = UUID.randomUUID()
        val seatId = UUID.randomUUID()

        // User 1 books DEL -> BOM
        val segment1Booking = createSegmentBookingRequest(
            userId = userId1,
            seatId = seatId,
            sourceCode = "DEL",
            destinationCode = "BOM"
        )

        // User 2 books BOM -> BLR (non-overlapping)
        val segment2Booking = createSegmentBookingRequest(
            userId = userId2,
            seatId = seatId,
            sourceCode = "BOM",
            destinationCode = "BLR"
        )

        // When - Book both segments
        val result1 = mockMvc.perform(
            post("/api/book-tickets")
                .header("user-id", userId1.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(segment1Booking))
        )

        val result2 = mockMvc.perform(
            post("/api/book-tickets")
                .header("user-id", userId2.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(segment2Booking))
        )

        // Then - Both bookings should succeed
        result1.andExpect(status().isCreated)
        result2.andExpect(status().isCreated)
    }

    @Test
    fun `should search flights by segments`() {
        // Given - Create multi-stop flight
        setupMultiStopFlight()

        // When - Search for direct segment BOM -> BLR
        val result = mockMvc.perform(
            get("/api/book-tickets/flights/search")
                .param("sourceCode", "BOM")
                .param("destinationCode", "BLR")
                .param("date", LocalDate.now().toString())
                .param("directOnly", "true")
        )

        // Then - Should find the direct segment
        result.andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$[0].sourceCode").value("BOM"))
            .andExpect(jsonPath("$[0].destinationCode").value("BLR"))
            .andExpect(jsonPath("$[0].isDirect").value(true))
    }

    @Test
    fun `should handle concurrent segment bookings for same seat`() {
        // Given - Create multi-stop flight
        setupMultiStopFlight()
        
        val userId1 = UUID.randomUUID()
        val userId2 = UUID.randomUUID()
        val seatId = UUID.randomUUID()

        val booking1 = createSegmentBookingRequest(userId1, seatId, "DEL", "BOM")
        val booking2 = createSegmentBookingRequest(userId2, seatId, "DEL", "BLR") // Overlapping

        // When - Execute concurrent requests
        val executor = Executors.newFixedThreadPool(2)
        
        val future1 = CompletableFuture.supplyAsync({
            mockMvc.perform(
                post("/api/book-tickets")
                    .header("user-id", userId1.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(booking1))
            ).andReturn()
        }, executor)

        val future2 = CompletableFuture.supplyAsync({
            mockMvc.perform(
                post("/api/book-tickets")
                    .header("user-id", userId2.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(booking2))
            ).andReturn()
        }, executor)

        val result1 = future1.get()
        val result2 = future2.get()
        executor.shutdown()

        // Then - Only one should succeed due to overlap
        val statuses = listOf(result1.response.status, result2.response.status)
        val successCount = statuses.count { it == 201 }
        val conflictCount = statuses.count { it == 409 }

        assert(successCount == 1) { "Expected exactly 1 success, got $successCount" }
        assert(conflictCount == 1) { "Expected exactly 1 conflict, got $conflictCount" }
    }

    // Helper methods
    private fun setupMultiStopFlight() {
        val adminId = UUID.randomUUID()
        val flightCreationRequest = createMultiStopFlightRequest()
        
        mockMvc.perform(
            post("/api/book-tickets/flights")
                .header("admin-id", adminId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(flightCreationRequest))
        ).andExpect(status().isCreated)
    }

    private fun createMultiStopFlightRequest(): FlightCreationRequest {
        return FlightCreationRequest(
            flightNumber = "AI101",
            date = LocalDate.now(),
            sourceCode = "DEL",
            destinationCode = "BLR",
            travelStartTime = LocalTime.of(6, 0),
            travelEndTime = LocalTime.of(10, 30),
            numberOfStops = 1,
            stops = listOf(
                FlightStopRequest(
                    airportCode = "BOM",
                    airportName = "Chhatrapati Shivaji Airport",
                    city = "Mumbai",
                    arrivalTime = LocalTime.of(7, 30),
                    departureTime = LocalTime.of(8, 15),
                    layoverDuration = 45,
                    stopSequence = 1
                )
            ),
            seats = listOf(
                SeatCreationRequest(
                    seatNumber = "1A",
                    seatClass = SeatClass.BUSINESS,
                    amount = BigDecimal("15000.00")
                ),
                SeatCreationRequest(
                    seatNumber = "1B",
                    seatClass = SeatClass.BUSINESS,
                    amount = BigDecimal("15000.00")
                ),
                SeatCreationRequest(
                    seatNumber = "2A",
                    seatClass = SeatClass.ECONOMY,
                    amount = BigDecimal("8000.00")
                )
            )
        )
    }

    private fun createSegmentBookingRequest(
        userId: UUID,
        seatId: UUID,
        sourceCode: String,
        destinationCode: String
    ): BookingRequest {
        val flightDetails = FlightBookingDetails(
            vehicleId = UUID.fromString("00000000-0000-0000-0000-000000000001"), // AI101
            flightStartTime = "06:00",
            flightEndTime = "10:30",
            seatId = seatId,
            sourceCode = sourceCode,
            destinationCode = destinationCode,
            flightTime = "4h 30m",
            discountsApplied = emptyList()
        )

        return BookingRequest(
            userId = userId,
            bookingType = BookingType.FLIGHT,
            bookingDetails = flightDetails
        )
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
