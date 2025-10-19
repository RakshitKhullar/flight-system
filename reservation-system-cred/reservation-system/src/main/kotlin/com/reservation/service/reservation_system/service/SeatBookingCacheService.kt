package com.reservation.service.reservation_system.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration

@Service
class SeatBookingCacheService @Autowired constructor(
    private val redisTemplate: RedisTemplate<String, String>
) {
    
    private val logger = LoggerFactory.getLogger(SeatBookingCacheService::class.java)
    
    companion object {
        private const val SEAT_BOOKING_PREFIX = "seat_booking:"
        private const val BLOCKED_STATUS = "BLOCKED"
        private const val DEFAULT_TTL_MINUTES = 10L // Auto-expire blocked seats after 10 minutes
    }
    
    fun generateSeatKey(flightId: String, seatId: String, flightTime: String): String {
        return "$flightId:$seatId:$flightTime"
    }
    
    fun isSeatBookingInProgress(seatKey: String): Boolean {
        val redisKey = "$SEAT_BOOKING_PREFIX$seatKey"
        val status = redisTemplate.opsForValue().get(redisKey)
        logger.debug("Checking seat booking status for key: $seatKey, status: $status")
        return status == BLOCKED_STATUS
    }
    
    fun blockSeatForBooking(seatKey: String): Boolean {
        val redisKey = "$SEAT_BOOKING_PREFIX$seatKey"
        logger.info("Attempting to block seat for booking: $seatKey")
        
        try {
            // Use setIfAbsent for atomic operation with TTL
            val success = redisTemplate.opsForValue().setIfAbsent(
                redisKey, 
                BLOCKED_STATUS, 
                Duration.ofMinutes(DEFAULT_TTL_MINUTES)
            ) ?: false
            
            return if (success) {
                logger.info("Successfully blocked seat for booking: $seatKey with TTL: ${DEFAULT_TTL_MINUTES}m")
                true
            } else {
                val existingStatus = redisTemplate.opsForValue().get(redisKey)
                logger.warn("Seat already blocked for booking: $seatKey, existing status: $existingStatus")
                false
            }
        } catch (e: Exception) {
            logger.error("Failed to block seat for booking: $seatKey", e)
            return false
        }
    }
    
    fun releaseSeatBooking(seatKey: String) {
        val redisKey = "$SEAT_BOOKING_PREFIX$seatKey"
        logger.info("Releasing seat booking: $seatKey")
        
        try {
            val deleted = redisTemplate.delete(redisKey)
            if (deleted) {
                logger.info("Successfully released seat booking: $seatKey")
            } else {
                logger.warn("Seat booking key not found for release: $seatKey")
            }
        } catch (e: Exception) {
            logger.error("Failed to release seat booking: $seatKey", e)
        }
    }
    
    fun getAllBlockedSeats(): Map<String, String> {
        return try {
            val pattern = "$SEAT_BOOKING_PREFIX*"
            val keys = redisTemplate.keys(pattern)
            
            if (keys.isNullOrEmpty()) {
                emptyMap()
            } else {
                val values = redisTemplate.opsForValue().multiGet(keys)
                keys.zip(values ?: emptyList()).associate { (key, value) ->
                    key.removePrefix(SEAT_BOOKING_PREFIX) to (value ?: "")
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to get all blocked seats", e)
            emptyMap()
        }
    }
    
    fun clearAllBlockedSeats() {
        logger.warn("Clearing all blocked seats from Redis cache")
        
        try {
            val pattern = "$SEAT_BOOKING_PREFIX*"
            val keys = redisTemplate.keys(pattern)
            
            if (!keys.isNullOrEmpty()) {
                val deletedCount = redisTemplate.delete(keys)
                logger.info("Cleared $deletedCount blocked seats from cache")
            } else {
                logger.info("No blocked seats found to clear")
            }
        } catch (e: Exception) {
            logger.error("Failed to clear all blocked seats", e)
        }
    }
    
    fun getSeatBookingStatus(seatKey: String): String? {
        val redisKey = "$SEAT_BOOKING_PREFIX$seatKey"
        return try {
            redisTemplate.opsForValue().get(redisKey)
        } catch (e: Exception) {
            logger.error("Failed to get seat booking status for key: $seatKey", e)
            null
        }
    }
    
    // Additional Redis-specific methods
    
    fun extendSeatBookingTTL(seatKey: String, additionalMinutes: Long): Boolean {
        val redisKey = "$SEAT_BOOKING_PREFIX$seatKey"
        return try {
            // Convert minutes to seconds for Redis expire command
            val timeoutSeconds = additionalMinutes * 60
            val success = redisTemplate.expire(redisKey, timeoutSeconds, java.util.concurrent.TimeUnit.SECONDS) ?: false
            logger.info("Extended TTL for seat booking: $seatKey by ${additionalMinutes}m, success: $success")
            success
        } catch (e: Exception) {
            logger.error("Failed to extend TTL for seat booking: $seatKey", e)
            false
        }
    }
    
    fun getSeatBookingTTL(seatKey: String): Long {
        val redisKey = "$SEAT_BOOKING_PREFIX$seatKey"
        return try {
            // getExpire returns Long? representing seconds, or null if key doesn't exist
            redisTemplate.getExpire(redisKey) ?: -1L
        } catch (e: Exception) {
            logger.error("Failed to get TTL for seat booking: $seatKey", e)
            -1L
        }
    }
    
    fun getRedisConnectionStatus(): Boolean {
        return try {
            val connectionFactory = redisTemplate.connectionFactory
            if (connectionFactory != null) {
                val connection = connectionFactory.connection
                val result = connection.ping()
                connection.close()
                result != null
            } else {
                false
            }
        } catch (e: Exception) {
            logger.error("Redis connection check failed", e)
            false
        }
    }
}
