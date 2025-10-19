package com.reservation.service.reservation_system.factory

import com.reservation.service.reservation_system.service.BookingService
import org.springframework.stereotype.Component

@Component
class BookingServiceFactory(private val bookingServices: List<BookingService>) {
    
    private val bookingServiceMap = bookingServices.associateBy { it.getBookingType() }
    
    fun getService(bookingType: String): BookingService {
        return bookingServiceMap[bookingType] 
            ?: throw IllegalArgumentException("No booking service found for type: $bookingType")
    }
}
