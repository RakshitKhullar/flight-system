package com.reservation.service.reservation_system.service

import io.mockk.*
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ValueOperations
import java.time.Duration
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@ExtendWith(MockKExtension::class)
class SeatBookingCacheServiceTest {

    private val redisTemplate = mockk<RedisTemplate<String, String>>()
    private val valueOperations = mockk<ValueOperations<String, String>>()

    private lateinit var seatBookingCacheService: SeatBookingCacheService

    private val flightId = "FL123"
    private val seatId = UUID.randomUUID().toString()
    private val flightTime = "10:30"
    private val seatKey = "$flightId:$seatId:$flightTime"
    private val redisKey = "seat_booking:$seatKey"

    @BeforeEach
    fun setUp() {
        clearAllMocks()
        every { redisTemplate.opsForValue() } returns valueOperations
        seatBookingCacheService = SeatBookingCacheService(redisTemplate)
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
        // Given
        every { valueOperations.get(redisKey) } returns null

        // When
        val result = seatBookingCacheService.isSeatBookingInProgress(seatKey)

        // Then
        assertFalse(result)
        verify { valueOperations.get(redisKey) }
    }

    @Test
    fun `should return true when seat is blocked`() {
        // Given
        every { valueOperations.get(redisKey) } returns "BLOCKED"

        // When
        val result = seatBookingCacheService.isSeatBookingInProgress(seatKey)

        // Then
        assertTrue(result)
        verify { valueOperations.get(redisKey) }
    }

    @Test
    fun `should block seat successfully when not already blocked`() {
        // Given
        every { 
            valueOperations.setIfAbsent(redisKey, "BLOCKED", Duration.ofMinutes(10)) 
        } returns true
        every { valueOperations.get(redisKey) } returns "BLOCKED"

        // When
        val result = seatBookingCacheService.blockSeatForBooking(seatKey)

        // Then
        assertTrue(result)
        verify { valueOperations.setIfAbsent(redisKey, "BLOCKED", Duration.ofMinutes(10)) }
    }

    @Test
    fun `should fail to block seat when already blocked`() {
        // Given
        every { 
            valueOperations.setIfAbsent(redisKey, "BLOCKED", Duration.ofMinutes(10)) 
        } returns false
        every { valueOperations.get(redisKey) } returns "BLOCKED"

        // When
        val result = seatBookingCacheService.blockSeatForBooking(seatKey)

        // Then
        assertFalse(result)
        verify { valueOperations.setIfAbsent(redisKey, "BLOCKED", Duration.ofMinutes(10)) }
        verify { valueOperations.get(redisKey) }
    }

    @Test
    fun `should release seat booking successfully`() {
        // Given
        every { redisTemplate.delete(redisKey) } returns true

        // When
        seatBookingCacheService.releaseSeatBooking(seatKey)

        // Then
        verify { redisTemplate.delete(redisKey) }
    }

    @Test
    fun `should get all blocked seats`() {
        // Given
        val seatKey1 = "FL123:seat1:10:30"
        val seatKey2 = "FL456:seat2:14:00"
        val redisKey1 = "seat_booking:$seatKey1"
        val redisKey2 = "seat_booking:$seatKey2"
        
        every { redisTemplate.keys("seat_booking:*") } returns setOf(redisKey1, redisKey2)
        every { valueOperations.multiGet(setOf(redisKey1, redisKey2)) } returns listOf("BLOCKED", "BLOCKED")

        // When
        val result = seatBookingCacheService.getAllBlockedSeats()

        // Then
        assertEquals(2, result.size)
        assertEquals("BLOCKED", result[seatKey1])
        assertEquals("BLOCKED", result[seatKey2])
        verify { redisTemplate.keys("seat_booking:*") }
        verify { valueOperations.multiGet(setOf(redisKey1, redisKey2)) }
    }

    @Test
    fun `should clear all blocked seats`() {
        // Given
        val redisKey1 = "seat_booking:FL123:seat1:10:30"
        val redisKey2 = "seat_booking:FL456:seat2:14:00"
        
        every { redisTemplate.keys("seat_booking:*") } returns setOf(redisKey1, redisKey2)
        every { redisTemplate.delete(setOf(redisKey1, redisKey2)) } returns 2L

        // When
        seatBookingCacheService.clearAllBlockedSeats()

        // Then
        verify { redisTemplate.keys("seat_booking:*") }
        verify { redisTemplate.delete(setOf(redisKey1, redisKey2)) }
    }

    @Test
    fun `should get seat booking status correctly`() {
        // Given - seat not blocked
        every { valueOperations.get(redisKey) } returns null

        // When
        val result1 = seatBookingCacheService.getSeatBookingStatus(seatKey)

        // Then
        assertNull(result1)

        // Given - seat blocked
        every { valueOperations.get(redisKey) } returns "BLOCKED"

        // When
        val result2 = seatBookingCacheService.getSeatBookingStatus(seatKey)

        // Then
        assertEquals("BLOCKED", result2)
        verify(exactly = 2) { valueOperations.get(redisKey) }
    }

    @Test
    fun `should extend seat booking TTL successfully`() {
        // Given
        every { redisTemplate.expire(redisKey, 300L, java.util.concurrent.TimeUnit.SECONDS) } returns true

        // When
        val result = seatBookingCacheService.extendSeatBookingTTL(seatKey, 5)

        // Then
        assertTrue(result)
        verify { redisTemplate.expire(redisKey, 300L, java.util.concurrent.TimeUnit.SECONDS) }
    }

    @Test
    fun `should get seat booking TTL correctly`() {
        // Given
        every { redisTemplate.getExpire(redisKey) } returns 480L // 8 minutes in seconds

        // When
        val result = seatBookingCacheService.getSeatBookingTTL(seatKey)

        // Then
        assertEquals(480L, result) // 8 minutes = 480 seconds
        verify { redisTemplate.getExpire(redisKey) }
    }

    @Test
    fun `should handle Redis operation failure gracefully`() {
        // Given
        every { 
            valueOperations.setIfAbsent(redisKey, "BLOCKED", Duration.ofMinutes(10)) 
        } throws RuntimeException("Redis connection failed")

        // When
        val result = seatBookingCacheService.blockSeatForBooking(seatKey)

        // Then
        assertFalse(result)
        verify { valueOperations.setIfAbsent(redisKey, "BLOCKED", Duration.ofMinutes(10)) }
    }

    @Test
    fun `should handle empty keys when getting all blocked seats`() {
        // Given
        every { redisTemplate.keys("seat_booking:*") } returns emptySet()

        // When
        val result = seatBookingCacheService.getAllBlockedSeats()

        // Then
        assertTrue(result.isEmpty())
        verify { redisTemplate.keys("seat_booking:*") }
    }

    @Test
    fun `should handle null TTL response`() {
        // Given
        every { redisTemplate.getExpire(redisKey) } returns -1L

        // When
        val result = seatBookingCacheService.getSeatBookingTTL(seatKey)

        // Then
        assertEquals(-1L, result)
        verify { redisTemplate.getExpire(redisKey) }
    }
}
