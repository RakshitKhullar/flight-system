package com.travel_search.travel_search_service.repository.entity

import com.fasterxml.jackson.databind.JsonNode
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.LocalDate
import java.time.ZonedDateTime

@Entity
@Table(name = "elastic_search")
data class ElasticSearch(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    
    @Column(name = "flight_id", nullable = false, length = 50)
    val flightId: String,
    
    @Column(name = "source", nullable = false, length = 100)
    val source: String,
    
    @Column(name = "destination", nullable = false, length = 100)
    val destination: String,
    
    @Column(name = "flightdate", nullable = false)
    val flightDate: LocalDate,
    
    @Column(name = "maximumstops", nullable = false)
    val maximumStops: Int = 0,
    
    @Column(name = "departner", length = 100)
    val departner: String? = null,
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "seat_structure", columnDefinition = "jsonb")
    val seatStructure: JsonNode? = null,
    
    @Column(name = "created_at", nullable = false)
    val createdAt: ZonedDateTime = ZonedDateTime.now(),
    
    @Column(name = "updated_at", nullable = false)
    var updatedAt: ZonedDateTime = ZonedDateTime.now()
) {
    @PreUpdate
    fun preUpdate() {
        updatedAt = ZonedDateTime.now()
    }
}
