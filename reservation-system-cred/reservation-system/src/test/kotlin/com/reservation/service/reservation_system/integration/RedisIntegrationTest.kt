package com.reservation.service.reservation_system.integration

import com.reservation.service.reservation_system.service.SeatBookingCacheService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import redis.embedded.RedisServer
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@SpringBootTest
@ActiveProfiles("test")
class RedisIntegrationTest {

    companion object {
        private lateinit var redisServer: RedisServer
        private const val REDIS_PORT = 6370 // Different port to avoid conflicts

        @JvmStatic
        @DynamicPropertySource
        fun configureProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.redis.host") { "localhost" }
            registry.add("spring.redis.port") { REDIS_PORT }
            registry.add("spring.redis.password") { "" }
        }
    }

    @Autowired
    private lateinit var seatBookingCacheService: SeatBookingCacheService

    @Autowired
    private lateinit var redisTemplate: RedisTemplate<String, String>

    private val flightId = "FL123"
    private val seatId = UUID.randomUUID().toString()
    private val flightTime = "10:30"
    private val seatKey = "$flightId:$seatId:$flightTime"

    @BeforeEach
    fun setUp() {
        try {
            redisServer = RedisServer.builder()
                .port(REDIS_PORT)
                .setting("maxmemory 128M")
                .build()
            redisServer.start()
        } catch (e: Exception) {
            // Redis server might already be running
            println("Redis server setup: ${e.message}")
        }

        // Clear any existing data
        seatBookingCacheService.clearAllBlockedSeats()
    }

    @AfterEach
    fun tearDown() {
        try {
            seatBookingCacheService.clearAllBlockedSeats()
            redisServer.stop()
        } catch (e: Exception) {
            // Ignore cleanup errors
            println("Redis server cleanup: ${e.message}")
        }
    }

    @Test
    fun `should generate seat key correctly`() {
        // When
        val result = seatBookingCacheService.generateSeatKey(flightId, seatId, flightTime)

        // Then
        assertEquals(seatKey, result)
    }

    @Test
    fun `should return false when seat is not blocked in Redis`() {
        // When
        val result = seatBookingCacheService.isSeatBookingInProgress(seatKey)

        // Then
        assertFalse(result)
    }

    @Test
    fun `should block and unblock seat successfully in Redis`() {
        // Given - seat is not blocked initially
        assertFalse(seatBookingCacheService.isSeatBookingInProgress(seatKey))

        // When - block the seat
        val blockResult = seatBookingCacheService.blockSeatForBooking(seatKey)

        // Then - seat should be blocked
        assertTrue(blockResult)
        assertTrue(seatBookingCacheService.isSeatBookingInProgress(seatKey))
        assertEquals("BLOCKED", seatBookingCacheService.getSeatBookingStatus(seatKey))

        // When - release the seat
        seatBookingCacheService.releaseSeatBooking(seatKey)

        // Then - seat should be available
        assertFalse(seatBookingCacheService.isSeatBookingInProgress(seatKey))
        assertNull(seatBookingCacheService.getSeatBookingStatus(seatKey))
    }

    @Test
    fun `should fail to block seat when already blocked in Redis`() {
        // Given - block the seat first
        assertTrue(seatBookingCacheService.blockSeatForBooking(seatKey))

        // When - try to block again
        val result = seatBookingCacheService.blockSeatForBooking(seatKey)

        // Then - should fail
        assertFalse(result)
        assertTrue(seatBookingCacheService.isSeatBookingInProgress(seatKey))
    }

    @Test
    fun `should handle multiple seats in Redis`() {
        // Given
        val seatKey1 = "FL123:seat1:10:30"
        val seatKey2 = "FL456:seat2:14:00"
        val seatKey3 = "FL789:seat3:18:00"

        // When - block multiple seats
        assertTrue(seatBookingCacheService.blockSeatForBooking(seatKey1))
        assertTrue(seatBookingCacheService.blockSeatForBooking(seatKey2))
        assertTrue(seatBookingCacheService.blockSeatForBooking(seatKey3))

        // Then - all should be blocked
        assertTrue(seatBookingCacheService.isSeatBookingInProgress(seatKey1))
        assertTrue(seatBookingCacheService.isSeatBookingInProgress(seatKey2))
        assertTrue(seatBookingCacheService.isSeatBookingInProgress(seatKey3))

        // When - get all blocked seats
        val allBlocked = seatBookingCacheService.getAllBlockedSeats()

        // Then - should contain all three
        assertEquals(3, allBlocked.size)
        assertEquals("BLOCKED", allBlocked[seatKey1])
        assertEquals("BLOCKED", allBlocked[seatKey2])
        assertEquals("BLOCKED", allBlocked[seatKey3])
    }

    @Test
    fun `should clear all blocked seats from Redis`() {
        // Given - block multiple seats
        val seatKey1 = "FL123:seat1:10:30"
        val seatKey2 = "FL456:seat2:14:00"
        
        seatBookingCacheService.blockSeatForBooking(seatKey1)
        seatBookingCacheService.blockSeatForBooking(seatKey2)
        assertEquals(2, seatBookingCacheService.getAllBlockedSeats().size)

        // When - clear all
        seatBookingCacheService.clearAllBlockedSeats()

        // Then - should be empty
        assertEquals(0, seatBookingCacheService.getAllBlockedSeats().size)
        assertFalse(seatBookingCacheService.isSeatBookingInProgress(seatKey1))
        assertFalse(seatBookingCacheService.isSeatBookingInProgress(seatKey2))
    }

    @Test
    fun `should handle TTL operations in Redis`() {
        // Given - block a seat
        assertTrue(seatBookingCacheService.blockSeatForBooking(seatKey))

        // When - get TTL (should be around 10 minutes = 600 seconds)
        val ttl = seatBookingCacheService.getSeatBookingTTL(seatKey)

        // Then - should have TTL set
        assertTrue(ttl > 0)
        assertTrue(ttl <= 600) // Should be less than or equal to 10 minutes

        // When - extend TTL
        val extendResult = seatBookingCacheService.extendSeatBookingTTL(seatKey, 5)

        // Then - should succeed
        assertTrue(extendResult)
    }

    @Test
    fun `should check Redis connection status`() {
        // When
        val isConnected = seatBookingCacheService.getRedisConnectionStatus()

        // Then
        assertTrue(isConnected)
    }

    @Test
    fun `should handle concurrent seat blocking in Redis`() {
        // Given
        val results = mutableListOf<Boolean>()

        // When - simulate concurrent blocking attempts
        repeat(5) {
            results.add(seatBookingCacheService.blockSeatForBooking(seatKey))
        }

        // Then - only one should succeed
        val successCount = results.count { it }
        assertEquals(1, successCount)
        assertTrue(seatBookingCacheService.isSeatBookingInProgress(seatKey))
    }

    @Test
    fun `should handle segment-specific seat keys in Redis`() {
        // Given - different segments for same flight
        val segmentKey1 = "AI101:${UUID.randomUUID()}:DEL:BOM"
        val segmentKey2 = "AI101:${UUID.randomUUID()}:BOM:BLR"
        val segmentKey3 = "AI101:${UUID.randomUUID()}:DEL:BLR"

        // When - block seats for different segments
        assertTrue(seatBookingCacheService.blockSeatForBooking(segmentKey1))
        assertTrue(seatBookingCacheService.blockSeatForBooking(segmentKey2))
        assertTrue(seatBookingCacheService.blockSeatForBooking(segmentKey3))

        // Then - all should be blocked independently
        assertTrue(seatBookingCacheService.isSeatBookingInProgress(segmentKey1))
        assertTrue(seatBookingCacheService.isSeatBookingInProgress(segmentKey2))
        assertTrue(seatBookingCacheService.isSeatBookingInProgress(segmentKey3))

        // When - release one segment
        seatBookingCacheService.releaseSeatBooking(segmentKey1)

        // Then - others should remain blocked
        assertFalse(seatBookingCacheService.isSeatBookingInProgress(segmentKey1))
        assertTrue(seatBookingCacheService.isSeatBookingInProgress(segmentKey2))
        assertTrue(seatBookingCacheService.isSeatBookingInProgress(segmentKey3))
    }

    @Test
    fun `should verify Redis key patterns and prefixes`() {
        // Given
        val testSeatKey = "TEST123:seat1:12:00"
        seatBookingCacheService.blockSeatForBooking(testSeatKey)

        // When - check raw Redis key exists with correct prefix
        val redisKey = "seat_booking:$testSeatKey"
        val rawValue = redisTemplate.opsForValue().get(redisKey)

        // Then
        assertEquals("BLOCKED", rawValue)
        assertTrue(seatBookingCacheService.isSeatBookingInProgress(testSeatKey))
    }
}
