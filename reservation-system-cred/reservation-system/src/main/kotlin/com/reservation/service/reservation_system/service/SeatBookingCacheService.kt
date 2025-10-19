package com.reservation.service.reservation_system.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

@Service
class SeatBookingCacheService {
    
    private val logger = LoggerFactory.getLogger(SeatBookingCacheService::class.java)
    
    // Global map to simulate Redis - key: "flightId:seatId:flightTime", value: "BLOCKED"
    private val seatBookingCache = ConcurrentHashMap<String, String>()
    
    fun generateSeatKey(flightId: String, seatId: String, flightTime: String): String {
        return "$flightId:$seatId:$flightTime"
    }
    
    fun isSeatBookingInProgress(seatKey: String): Boolean {
        val status = seatBookingCache[seatKey]
        logger.debug("Checking seat booking status for key: $seatKey, status: $status")
        return status == "BLOCKED"
    }
    
    fun blockSeatForBooking(seatKey: String): Boolean {
        logger.info("Attempting to block seat for booking: $seatKey")
        
        // Use putIfAbsent for atomic operation - returns null if key was not present
        val previousValue = seatBookingCache.putIfAbsent(seatKey, "BLOCKED")
        
        return if (previousValue == null) {
            logger.info("Successfully blocked seat for booking: $seatKey")
            true
        } else {
            logger.warn("Seat already blocked for booking: $seatKey, existing status: $previousValue")
            false
        }
    }
    
    fun releaseSeatBooking(seatKey: String) {
        logger.info("Releasing seat booking: $seatKey")
        seatBookingCache.remove(seatKey)
    }
    
    fun getAllBlockedSeats(): Map<String, String> {
        return seatBookingCache.toMap()
    }
    
    fun clearAllBlockedSeats() {
        logger.warn("Clearing all blocked seats from cache")
        seatBookingCache.clear()
    }
    
    fun getSeatBookingStatus(seatKey: String): String? {
        return seatBookingCache[seatKey]
    }
}
