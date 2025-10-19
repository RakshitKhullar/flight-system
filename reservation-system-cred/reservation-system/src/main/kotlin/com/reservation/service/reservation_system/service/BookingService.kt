package com.reservation.service.reservation_system.service

import com.reservation.service.reservation_system.dto.BookingRequest
import com.reservation.service.reservation_system.repository.entity.Ticket
import java.util.UUID

interface BookingService {
    fun getBookingType(): String
    fun bookTicket(bookingRequest: BookingRequest): Ticket
    fun cancelBooking(bookingId: UUID, userId: UUID): Boolean
    fun getBookingDetails(bookingId: UUID, userId: UUID): Ticket?
}
