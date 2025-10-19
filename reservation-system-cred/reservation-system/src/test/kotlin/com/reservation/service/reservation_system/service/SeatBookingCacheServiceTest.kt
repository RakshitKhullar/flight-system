package com.reservation.service.reservation_system.service

import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@ExtendWith(MockKExtension::class)
class SeatBookingCacheServiceTest {

    private lateinit var seatBookingCacheService: SeatBookingCacheService

    private val flightId = "FL123"
    private val seatId = UUID.randomUUID().toString()
    private val flightTime = "10:30"
    private val seatKey = "$flightId:$seatId:$flightTime"

    @BeforeEach
    fun setUp() {
        seatBookingCacheService = SeatBookingCacheService()
    }

    @Test
    fun `should generate seat key correctly`() {
        // When
        val result = seatBookingCacheService.generateSeatKey(flightId, seatId, flightTime)

        // Then
        assertEquals(seatKey, result)
    }

    @Test
    fun `should return false when seat is not blocked`() {
        // When
        val result = seatBookingCacheService.isSeatBookingInProgress(seatKey)

        // Then
        assertFalse(result)
    }

    @Test
    fun `should return true when seat is blocked`() {
        // Given
        seatBookingCacheService.blockSeatForBooking(seatKey)

        // When
        val result = seatBookingCacheService.isSeatBookingInProgress(seatKey)

        // Then
        assertTrue(result)
    }

    @Test
    fun `should block seat successfully when not already blocked`() {
        // When
        val result = seatBookingCacheService.blockSeatForBooking(seatKey)

        // Then
        assertTrue(result)
        assertTrue(seatBookingCacheService.isSeatBookingInProgress(seatKey))
    }

    @Test
    fun `should fail to block seat when already blocked`() {
        // Given
        seatBookingCacheService.blockSeatForBooking(seatKey)

        // When
        val result = seatBookingCacheService.blockSeatForBooking(seatKey)

        // Then
        assertFalse(result)
        assertTrue(seatBookingCacheService.isSeatBookingInProgress(seatKey))
    }

    @Test
    fun `should release seat booking successfully`() {
        // Given
        seatBookingCacheService.blockSeatForBooking(seatKey)
        assertTrue(seatBookingCacheService.isSeatBookingInProgress(seatKey))

        // When
        seatBookingCacheService.releaseSeatBooking(seatKey)

        // Then
        assertFalse(seatBookingCacheService.isSeatBookingInProgress(seatKey))
    }

    @Test
    fun `should get all blocked seats`() {
        // Given
        val seatKey1 = "FL123:seat1:10:30"
        val seatKey2 = "FL456:seat2:14:00"
        
        seatBookingCacheService.blockSeatForBooking(seatKey1)
        seatBookingCacheService.blockSeatForBooking(seatKey2)

        // When
        val result = seatBookingCacheService.getAllBlockedSeats()

        // Then
        assertEquals(2, result.size)
        assertEquals("BLOCKED", result[seatKey1])
        assertEquals("BLOCKED", result[seatKey2])
    }

    @Test
    fun `should clear all blocked seats`() {
        // Given
        val seatKey1 = "FL123:seat1:10:30"
        val seatKey2 = "FL456:seat2:14:00"
        
        seatBookingCacheService.blockSeatForBooking(seatKey1)
        seatBookingCacheService.blockSeatForBooking(seatKey2)
        assertEquals(2, seatBookingCacheService.getAllBlockedSeats().size)

        // When
        seatBookingCacheService.clearAllBlockedSeats()

        // Then
        assertEquals(0, seatBookingCacheService.getAllBlockedSeats().size)
        assertFalse(seatBookingCacheService.isSeatBookingInProgress(seatKey1))
        assertFalse(seatBookingCacheService.isSeatBookingInProgress(seatKey2))
    }

    @Test
    fun `should get seat booking status correctly`() {
        // Given - seat not blocked
        assertNull(seatBookingCacheService.getSeatBookingStatus(seatKey))

        // When - block the seat
        seatBookingCacheService.blockSeatForBooking(seatKey)

        // Then
        assertEquals("BLOCKED", seatBookingCacheService.getSeatBookingStatus(seatKey))
    }

    @Test
    fun `should handle concurrent blocking attempts`() {
        // Given
        val results = mutableListOf<Boolean>()

        // When - simulate concurrent blocking attempts
        repeat(10) {
            results.add(seatBookingCacheService.blockSeatForBooking(seatKey))
        }

        // Then - only one should succeed
        val successCount = results.count { it }
        assertEquals(1, successCount)
        assertTrue(seatBookingCacheService.isSeatBookingInProgress(seatKey))
    }

    @Test
    fun `should handle multiple different seats`() {
        // Given
        val seatKey1 = "FL123:seat1:10:30"
        val seatKey2 = "FL123:seat2:10:30"
        val seatKey3 = "FL456:seat1:14:00"

        // When
        val result1 = seatBookingCacheService.blockSeatForBooking(seatKey1)
        val result2 = seatBookingCacheService.blockSeatForBooking(seatKey2)
        val result3 = seatBookingCacheService.blockSeatForBooking(seatKey3)

        // Then
        assertTrue(result1)
        assertTrue(result2)
        assertTrue(result3)
        
        assertTrue(seatBookingCacheService.isSeatBookingInProgress(seatKey1))
        assertTrue(seatBookingCacheService.isSeatBookingInProgress(seatKey2))
        assertTrue(seatBookingCacheService.isSeatBookingInProgress(seatKey3))
        
        assertEquals(3, seatBookingCacheService.getAllBlockedSeats().size)
    }

    @Test
    fun `should release specific seat without affecting others`() {
        // Given
        val seatKey1 = "FL123:seat1:10:30"
        val seatKey2 = "FL123:seat2:10:30"
        
        seatBookingCacheService.blockSeatForBooking(seatKey1)
        seatBookingCacheService.blockSeatForBooking(seatKey2)

        // When
        seatBookingCacheService.releaseSeatBooking(seatKey1)

        // Then
        assertFalse(seatBookingCacheService.isSeatBookingInProgress(seatKey1))
        assertTrue(seatBookingCacheService.isSeatBookingInProgress(seatKey2))
        assertEquals(1, seatBookingCacheService.getAllBlockedSeats().size)
    }
}
