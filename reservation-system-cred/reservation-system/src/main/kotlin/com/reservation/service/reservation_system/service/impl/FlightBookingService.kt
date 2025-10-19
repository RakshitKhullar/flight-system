package com.reservation.service.reservation_system.service.impl

import com.reservation.service.reservation_system.dto.BookingRequest
import com.reservation.service.reservation_system.dto.BookingType
import com.reservation.service.reservation_system.dto.FlightBookingDetails
import com.reservation.service.reservation_system.dto.PaymentResponse
import com.reservation.service.reservation_system.repository.entity.BookingStatus
import com.reservation.service.reservation_system.repository.entity.PaymentStatus
import com.reservation.service.reservation_system.repository.TicketRepository
import com.reservation.service.reservation_system.repository.entity.Ticket
import com.reservation.service.reservation_system.repository.entity.cassandra.SeatInfo
import com.reservation.service.reservation_system.repository.entity.cassandra.SeatStatus
import com.reservation.service.reservation_system.service.BookingService
import com.reservation.service.reservation_system.service.FlightScheduleService
import com.reservation.service.reservation_system.service.PaymentService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID
import kotlin.random.Random

@Service
class FlightBookingService(
    private val ticketRepository: TicketRepository,
    private val flightScheduleService: FlightScheduleService,
    private val paymentService: PaymentService
) : BookingService {
    
    private val logger = LoggerFactory.getLogger(FlightBookingService::class.java)

    override fun getBookingType() = BookingType.FLIGHT.name

    @Transactional
    override fun bookTicket(bookingRequest: BookingRequest): Ticket {
        val flightDetails = bookingRequest.bookingDetails as FlightBookingDetails
        
        logger.info("Starting booking process for user: ${bookingRequest.userId}, seat: ${flightDetails.seatId}")
        
        // Step 1: Get available seats
        val availableSeats = flightScheduleService.getAvailableSeats(flightDetails.vehicleId.toString(), null)
        if (availableSeats.isEmpty()) {
            throw IllegalStateException("No available seats found for the requested flight")
        }
        
        // Step 2: Check if requested seat is available
        val requestedSeat = availableSeats.find { it.seatId == flightDetails.seatId }
            ?: throw IllegalStateException("Requested seat is not available")
        
        // Step 3: Block the seat
        val updatedSchedule = flightScheduleService.updateSeatStatusByFlightId(
            flightDetails.vehicleId.toString(), 
            flightDetails.seatId, 
            SeatStatus.BLOCKED
        ) ?: throw IllegalStateException("Failed to block seat")
        
        try {
            // Step 4: Create ticket
            val ticket = createTicket(bookingRequest, flightDetails, requestedSeat)
            
            // Step 5: Initiate payment
            val paymentResponse = paymentService.initiatePayment(ticket)
            
            // Step 6: Update ticket with payment details
            val updatedTicket = updateTicketWithPayment(ticket, paymentResponse)
            
            logger.info("Booking completed successfully for PNR: ${updatedTicket.pnrNumber}")
            return updatedTicket
            
        } catch (e: Exception) {
            // Rollback: Unblock the seat if payment fails
            logger.error("Booking failed, rolling back seat block for seat: ${flightDetails.seatId}", e)
            flightScheduleService.updateSeatStatusByFlightId(
                flightDetails.vehicleId.toString(), 
                flightDetails.seatId, 
                SeatStatus.AVAILABLE
            )
            throw e
        }
    }
    
    private fun createTicket(
        bookingRequest: BookingRequest,
        flightDetails: FlightBookingDetails,
        seat: SeatInfo
    ): Ticket {
        val pnrNumber = generatePnrNumber()
        
        val ticket = Ticket(
            userId = bookingRequest.userId,
            pnrNumber = pnrNumber,
            bookingType = BookingType.FLIGHT,
            bookingStatus = BookingStatus.PENDING,
            paymentStatus = PaymentStatus.PAYMENT_PENDING,
            flightId = flightDetails.vehicleId.toString(),
            seatId = seat.seatId,
            totalAmount = seat.amount,
            bookingDetails = null
        )
        
        return ticketRepository.save(ticket)
    }
    
    private fun updateTicketWithPayment(
        ticket: Ticket,
        paymentResponse: PaymentResponse
    ): Ticket {
        val updatedTicket = ticket.copy(
            paymentStatus = paymentResponse.status,
            bookingStatus = when (paymentResponse.status) {
                PaymentStatus.PAYMENT_COMPLETED -> BookingStatus.CONFIRMED
                PaymentStatus.PAYMENT_PENDING -> BookingStatus.PENDING
                else -> BookingStatus.FAILED
            }
        )
        
        return ticketRepository.save(updatedTicket)
    }
    
    private fun generatePnrNumber(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..6)
            .map { chars[Random.nextInt(chars.length)] }
            .joinToString("")
    }

    override fun cancelBooking(bookingId: UUID, userId: UUID): Boolean {
        val ticket = ticketRepository.findByTicketIdAndUserId(bookingId, userId) ?: return false
        
        val updatedTicket = ticket.copy(bookingStatus = BookingStatus.CANCELLED)
        ticketRepository.save(updatedTicket)
        return true
    }

    override fun getBookingDetails(bookingId: UUID, userId: UUID): Ticket? {
        return ticketRepository.findByTicketIdAndUserId(bookingId, userId)
    }
}
