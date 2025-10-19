package com.reservation.service.reservation_system.repository.cassandra

import com.reservation.service.reservation_system.repository.entity.cassandra.TravelSchedule
import org.springframework.data.cassandra.repository.CassandraRepository
import org.springframework.data.cassandra.repository.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface FlightScheduleRepository : CassandraRepository<TravelSchedule, UUID> {
    
    fun findByFlightId(flightId: String): List<TravelSchedule>
    
    @Query("SELECT * FROM flight_schedules WHERE flight_id = ?0 ALLOW FILTERING")
    fun findSchedulesByFlightId(flightId: String): List<TravelSchedule>
    
    fun existsByFlightId(flightId: String): Boolean
}
