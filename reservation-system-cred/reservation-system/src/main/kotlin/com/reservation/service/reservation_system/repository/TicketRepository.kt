package com.reservation.service.reservation_system.repository

import com.reservation.service.reservation_system.repository.entity.Ticket
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface TicketRepository : JpaRepository<Ticket, UUID> {
    
    fun findByTicketIdAndUserId(ticketId: UUID, userId: UUID): Ticket?
    
    fun findByPnrNumber(pnrNumber: String): Ticket?
    
    fun findByUserId(userId: UUID): List<Ticket>
}