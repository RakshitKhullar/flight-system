package com.reservation.service.reservation_system.handler

import com.reservation.service.reservation_system.dto.BookingRequest
import com.reservation.service.reservation_system.dto.BookingType
import com.reservation.service.reservation_system.exception.SeatBookingInProgressException
import com.reservation.service.reservation_system.factory.BookingServiceFactory
import com.reservation.service.reservation_system.repository.entity.Ticket
import com.reservation.service.reservation_system.service.SeatBookingCacheService
import com.reservation.service.reservation_system.service.SeatKeyService
import kotlinx.coroutines.delay
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.UUID
import kotlin.random.Random

@Component
class BookingHandler(
    private val bookingServiceFactory: BookingServiceFactory,
    private val seatBookingCacheService: SeatBookingCacheService,
    private val seatKeyService: SeatKeyService
) {
    
    private val logger = LoggerFactory.getLogger(BookingHandler::class.java)

    suspend fun processBooking(bookingRequest: BookingRequest): Ticket {
        // Only apply seat booking cache logic for FLIGHT bookings
        if (bookingRequest.bookingType == BookingType.FLIGHT) {
            return processFlightBookingWithCache(bookingRequest)
        }
        
        // For other booking types, proceed normally
        val bookingService = bookingServiceFactory.getService(bookingRequest.bookingType.name)
        return bookingService.bookTicket(bookingRequest)
    }
    /**
     * we add the request with some key in the cache and then we check if the seat is already booked or not
     * if the seat is not booked then we proceed with the booking else we throw an exception
     *
     * redis provide functionality to release an event when ttl is reached for the request
     * currently that part is not covered in the scope
     * have build a backdoor api to free data from cache and update the db as well with /unblock-seat in BookingStatusController controller
     */
    private suspend fun processFlightBookingWithCache(bookingRequest: BookingRequest): Ticket {
        val bookingDetails = bookingRequest.bookingDetails
        
        // Generate seat key using respective service based on booking type
        val seatKey = seatKeyService.generateSeatKey(bookingDetails)
        logger.info("Processing flight booking for seat key: $seatKey")
        
        // Step 1: Check if seat booking is already in progress
        if (seatBookingCacheService.isSeatBookingInProgress(seatKey)) {
            logger.warn("Seat booking already in progress for key: $seatKey")
            throw SeatBookingInProgressException("Seat booking in progress, kindly select another seat")
        }
        
        // Step 2: Generate random delay between 1-40ms
        val randomDelay = Random.nextLong(1, 41)
        logger.debug("Applying random delay of ${randomDelay}ms for seat key: $seatKey")
        delay(randomDelay)
        
        // Step 3: Check again after delay
        if (seatBookingCacheService.isSeatBookingInProgress(seatKey)) {
            logger.warn("Seat booking started during delay for key: $seatKey")
            throw SeatBookingInProgressException("Seat booking in progress, kindly select another seat")
        }
        
        // Step 4: Try to block the seat atomically
        val blocked = seatBookingCacheService.blockSeatForBooking(seatKey)
        if (!blocked) {
            logger.warn("Failed to block seat for booking, already blocked by another process: $seatKey")
            throw SeatBookingInProgressException("Seat booking in progress, kindly select another seat")
        }
        
        try {
            // Step 5: Proceed with actual booking
            logger.info("Proceeding with seat booking for key: $seatKey")
            val bookingService = bookingServiceFactory.getService(bookingRequest.bookingType.name)
            val ticket = bookingService.bookTicket(bookingRequest)
            
            logger.info("Successfully completed booking for seat key: $seatKey, ticket: ${ticket.ticketId}")
            return ticket
            
        } catch (e: Exception) {
            logger.error("Booking failed for seat key: $seatKey", e)
            throw e
        } finally {
            // Step 6: Always release the seat from cache after booking (success or failure)
            seatBookingCacheService.releaseSeatBooking(seatKey)
            logger.info("Released seat booking cache for key: $seatKey")
        }
    }

    fun processCancellation(bookingId: UUID, userId: UUID, bookingType: String): Boolean {
        val bookingService = bookingServiceFactory.getService(bookingType)
        return bookingService.cancelBooking(bookingId, userId)
    }

    fun getBookingDetails(bookingId: UUID, userId: UUID, bookingType: String): Ticket? {
        val bookingService = bookingServiceFactory.getService(bookingType)
        return bookingService.getBookingDetails(bookingId, userId)
    }
}
