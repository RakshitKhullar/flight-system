package com.reservation.service.reservation_system.service

import com.reservation.service.reservation_system.repository.entity.cassandra.TravelSchedule

interface AsyncNotificationService {
    
    suspend fun notifyScheduleSaved(travelSchedule: TravelSchedule)
}
