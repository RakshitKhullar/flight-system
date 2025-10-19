package com.reservation.service.reservation_system.store

import com.reservation.service.reservation_system.repository.entity.cassandra.TravelSchedule
import java.util.UUID

interface TravelScheduleStore {
    
    fun save(travelSchedule: TravelSchedule): TravelSchedule
    
    fun findById(id: UUID): TravelSchedule?
    
    fun findByFlightId(flightId: String): List<TravelSchedule>
    
    fun findAll(): List<TravelSchedule>
    
    fun existsById(id: UUID): Boolean
    
    fun deleteById(id: UUID)
    
    fun delete(travelSchedule: TravelSchedule)
}
