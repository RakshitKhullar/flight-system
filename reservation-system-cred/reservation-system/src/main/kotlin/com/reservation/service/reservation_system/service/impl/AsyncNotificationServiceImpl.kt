package com.reservation.service.reservation_system.service.impl

import com.reservation.service.reservation_system.repository.entity.cassandra.TravelSchedule
import com.reservation.service.reservation_system.service.AsyncNotificationService
import kotlinx.coroutines.delay
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate

@Service
class AsyncNotificationServiceImpl(
    private val restTemplate: RestTemplate
) : AsyncNotificationService {
    
    private val logger = LoggerFactory.getLogger(AsyncNotificationServiceImpl::class.java)
    
    @Value("\${internal.notification.api.base-url:http://localhost:8081}")
    private lateinit var notificationApiBaseUrl: String
    
    override suspend fun notifyScheduleSaved(travelSchedule: TravelSchedule) {
        logger.info("Sending async notification for schedule save: ${travelSchedule.id}")
        
        val payload = mapOf(
            "eventType" to "SCHEDULE_SAVED",
            "scheduleId" to travelSchedule.id.toString(),
            "flightId" to travelSchedule.vehicleId,
            "timestamp" to System.currentTimeMillis(),
            "data" to mapOf(
                "totalSeats" to travelSchedule.schedule.sumOf { it.totalSeats },
                "availableSeats" to travelSchedule.schedule.sumOf { it.availableSeats },
                "scheduleItems" to travelSchedule.schedule.size
            )
        )
        
        // Optional: Add small delay to simulate async processing
        delay(100)
        
        sendAsyncRequest("/api/internal/schedule-events", payload)
    }
    
    private fun sendAsyncRequest(endpoint: String, payload: Map<String, Any>) {
        try {
            val headers = HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
                set("X-Internal-Request", "true")
            }
            
            val request = HttpEntity(payload, headers)
            val url = "$notificationApiBaseUrl$endpoint"
            
            logger.debug("Sending async request to: $url")
            
            val response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                request,
                String::class.java
            )
            
            logger.debug("Async request completed with status: ${response.statusCode}")
            
        } catch (e: Exception) {
            logger.error("Failed to send async notification to $endpoint", e)
            // Don't rethrow - async failures shouldn't affect main flow
        }
    }
}
