package com.reservation.service.reservation_system.repository.entity.cassandra

import org.springframework.data.cassandra.core.mapping.Column
import org.springframework.data.cassandra.core.mapping.PrimaryKey
import org.springframework.data.cassandra.core.mapping.Table
import java.util.UUID

@Table("travel_schedules")
data class TravelSchedule(
    @PrimaryKey
    val id: UUID = UUID.randomUUID(),

    @Column("vehicle_id")
    val vehicleId: String,

    @Column("schedule")
    val schedule: List<ScheduleItem> = emptyList()
)