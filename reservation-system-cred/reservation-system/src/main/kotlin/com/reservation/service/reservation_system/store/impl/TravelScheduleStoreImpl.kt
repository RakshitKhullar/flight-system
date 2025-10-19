package com.reservation.service.reservation_system.store.impl

import com.reservation.service.reservation_system.repository.cassandra.FlightScheduleRepository
import com.reservation.service.reservation_system.repository.entity.cassandra.TravelSchedule
import com.reservation.service.reservation_system.store.TravelScheduleStore
import com.reservation.service.reservation_system.service.AsyncNotificationService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class TravelScheduleStoreImpl(
    private val flightScheduleRepository: FlightScheduleRepository,
    private val asyncNotificationService: AsyncNotificationService
) : TravelScheduleStore {

    private val logger = LoggerFactory.getLogger(TravelScheduleStoreImpl::class.java)
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    
    override fun save(travelSchedule: TravelSchedule): TravelSchedule {
        logger.info("Saving travel schedule: ${travelSchedule.id}")
        
        // 1. Save to repository first
        val savedSchedule = flightScheduleRepository.save(travelSchedule)
        
        // 2. Launch coroutine for async notification
        coroutineScope.launch {
            try {
                asyncNotificationService.notifyScheduleSaved(savedSchedule)
            } catch (e: Exception) {
                logger.error("Failed to send async notification for schedule: ${savedSchedule.id}", e)
                // Don't fail the main operation if async call fails
            }
        }
        
        return savedSchedule
    }
    
    override fun findById(id: UUID): TravelSchedule? {
        logger.debug("Finding travel schedule by id: $id")
        return flightScheduleRepository.findById(id).orElse(null)
    }
    
    override fun findByFlightId(flightId: String): List<TravelSchedule> {
        logger.debug("Finding travel schedules by flight id: $flightId")
        return flightScheduleRepository.findByFlightId(flightId)
    }
    
    override fun findAll(): List<TravelSchedule> {
        logger.debug("Finding all travel schedules")
        return flightScheduleRepository.findAll().toList()
    }
    
    override fun existsById(id: UUID): Boolean {
        logger.debug("Checking if travel schedule exists by id: $id")
        return flightScheduleRepository.existsById(id)
    }
    
    override fun deleteById(id: UUID) {
        logger.info("Deleting travel schedule by id: $id")
        flightScheduleRepository.deleteById(id)
    }
    
    override fun delete(travelSchedule: TravelSchedule) {
        logger.info("Deleting travel schedule: ${travelSchedule.id}")
        flightScheduleRepository.delete(travelSchedule)
    }
}
