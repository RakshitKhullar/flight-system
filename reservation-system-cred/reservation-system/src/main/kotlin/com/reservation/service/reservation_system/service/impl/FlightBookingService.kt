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
        
        logger.info("Starting segment booking process for user: ${bookingRequest.userId}, seat: ${flightDetails.seatId}, route: ${flightDetails.sourceCode} -> ${flightDetails.destinationCode}")
        
        // Step 1: Get flight number from vehicleId (assuming vehicleId contains flight number)
        val flightNumber = flightDetails.vehicleId.toString()
        
        // Step 2: Get available seats for the specific segment
        val availableSeats = flightScheduleService.getAvailableSeats(flightNumber, null)
        if (availableSeats.isEmpty()) {
            throw IllegalStateException("No available seats found for the requested flight")
        }
        
        // Step 3: Check if requested seat is available
        val requestedSeat = availableSeats.find { it.seatId == flightDetails.seatId }
            ?: throw IllegalStateException("Requested seat is not available")
        
        // Step 4: Block the seat for the specific segment using new segment booking logic
        val segmentBooked = flightScheduleService.bookSeatForSegment(
            flightNumber = flightNumber,
            date = java.time.LocalDate.now(), // You may want to get this from flightDetails
            seatId = flightDetails.seatId,
            sourceCode = flightDetails.sourceCode,
            destinationCode = flightDetails.destinationCode
        )
        
        if (!segmentBooked) {
            throw IllegalStateException("Failed to book seat for the requested segment. Seat may be unavailable for this route.")
        }
        
        try {
            // Step 5: Create ticket with segment information
            val ticket = createTicketWithSegment(bookingRequest, flightDetails, requestedSeat)
            
            // Step 6: Initiate payment
            val paymentResponse = paymentService.initiatePayment(ticket)
            
            // Step 7: Update ticket with payment details
            val updatedTicket = updateTicketWithPayment(ticket, paymentResponse)
            
            // Step 8: Confirm the seat booking in all overlapping segments
            flightScheduleService.confirmBlockedSeatByFlightId(flightNumber, flightDetails.seatId)
            
            logger.info("Segment booking completed successfully for PNR: ${updatedTicket.pnrNumber}, route: ${flightDetails.sourceCode} -> ${flightDetails.destinationCode}")
            return updatedTicket
            
        } catch (e: Exception) {
            // Rollback: Release the seat booking for the segment if payment fails
            logger.error("Segment booking failed, rolling back seat booking for seat: ${flightDetails.seatId}, route: ${flightDetails.sourceCode} -> ${flightDetails.destinationCode}", e)
            rollbackSegmentBooking(flightNumber, flightDetails)
            throw e
        }
    }
    
    private fun createTicketWithSegment(
        bookingRequest: BookingRequest,
        flightDetails: FlightBookingDetails,
        seat: SeatInfo
    ): Ticket {
        val pnrNumber = generatePnrNumber()
        
        // Create booking details JSON with segment information
        val segmentBookingDetails = mapOf(
            "sourceCode" to flightDetails.sourceCode,
            "destinationCode" to flightDetails.destinationCode,
            "flightStartTime" to flightDetails.flightStartTime,
            "flightEndTime" to flightDetails.flightEndTime,
            "seatNumber" to seat.seatNumber,
            "seatClass" to seat.seatClass.name,
            "discountsApplied" to flightDetails.discountsApplied
        )
        
        val ticket = Ticket(
            userId = bookingRequest.userId,
            pnrNumber = pnrNumber,
            bookingType = BookingType.FLIGHT,
            bookingStatus = BookingStatus.PENDING,
            paymentStatus = PaymentStatus.PAYMENT_PENDING,
            flightId = flightDetails.vehicleId.toString(),
            seatId = seat.seatId,
            totalAmount = seat.amount,
            bookingDetails = segmentBookingDetails.toString()
        )
        
        return ticketRepository.save(ticket)
    }
    
    private fun rollbackSegmentBooking(flightNumber: String, flightDetails: FlightBookingDetails) {
        try {
            // Find all segments that were booked and release them
            val schedules = flightScheduleService.getFlightSchedules(flightNumber)
            schedules.forEach { schedule ->
                schedule.schedule.forEach { scheduleItem ->
                    if (isSegmentAffected(scheduleItem, flightDetails.sourceCode, flightDetails.destinationCode)) {
                        flightScheduleService.updateSeatStatus(
                            scheduleItem.scheduleId,
                            flightDetails.seatId,
                            SeatStatus.AVAILABLE
                        )
                    }
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to rollback segment booking", e)
        }
    }
    
    private fun isSegmentAffected(
        scheduleItem: com.reservation.service.reservation_system.repository.entity.cassandra.ScheduleItem,
        requestedSource: String,
        requestedDestination: String
    ): Boolean {
        // Get all airports in the schedule item route
        val scheduleAirports = mutableListOf<String>()
        scheduleAirports.add(scheduleItem.sourceCode)
        scheduleAirports.addAll(scheduleItem.stops.sortedBy { it.stopSequence }.map { it.airportCode })
        scheduleAirports.add(scheduleItem.destinationCode)

        // Check if there's any overlap with the requested segment
        val requestedSourceIndex = scheduleAirports.indexOf(requestedSource)
        val requestedDestinationIndex = scheduleAirports.indexOf(requestedDestination)
        val scheduleSourceIndex = scheduleAirports.indexOf(scheduleItem.sourceCode)
        val scheduleDestinationIndex = scheduleAirports.indexOf(scheduleItem.destinationCode)

        // If either airport is not found, no overlap
        if (requestedSourceIndex == -1 || requestedDestinationIndex == -1 ||
            scheduleSourceIndex == -1 || scheduleDestinationIndex == -1) {
            return false
        }

        // Check if the segments overlap
        return !(requestedDestinationIndex <= scheduleSourceIndex || 
                 requestedSourceIndex >= scheduleDestinationIndex)
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
