package com.travel_search.travel_search_service.repository

import com.travel_search.travel_search_service.repository.entity.ElasticSearch
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface ElasticSearchRepository : JpaRepository<ElasticSearch, Long> {
    
    fun findByFlightId(flightId: String): ElasticSearch?
    
    fun findAllByFlightId(flightId: String): List<ElasticSearch>
    
    fun findBySourceAndDestinationAndFlightDate(
        source: String, 
        destination: String, 
        flightDate: LocalDate
    ): List<ElasticSearch>
    
    fun findBySourceAndDestinationAndFlightDateAndMaximumStopsLessThanEqual(
        source: String,
        destination: String,
        flightDate: LocalDate,
        maximumStops: Int
    ): List<ElasticSearch>
    
    fun findByDepartner(departner: String): List<ElasticSearch>
    
    @Query("SELECT e FROM ElasticSearch e WHERE e.source = :source AND e.destination = :destination " +
           "AND e.flightDate = :flightDate AND (:departner IS NULL OR e.departner = :departner) " +
           "AND e.maximumStops <= :maxStops")
    fun findFlightsBySearchCriteria(
        @Param("source") source: String,
        @Param("destination") destination: String,
        @Param("flightDate") flightDate: LocalDate,
        @Param("departner") departner: String?,
        @Param("maxStops") maxStops: Int
    ): List<ElasticSearch>
    
    @Query("SELECT DISTINCT e.source FROM ElasticSearch e ORDER BY e.source")
    fun findAllSources(): List<String>
    
    @Query("SELECT DISTINCT e.destination FROM ElasticSearch e ORDER BY e.destination")
    fun findAllDestinations(): List<String>
    
    @Query("SELECT DISTINCT e.departner FROM ElasticSearch e WHERE e.departner IS NOT NULL ORDER BY e.departner")
    fun findAllDepartners(): List<String>
    
    @Query("SELECT e FROM ElasticSearch e WHERE e.flightId = :flightId AND e.source = :source " +
           "AND e.destination = :destination AND e.flightDate = :flightDate " +
           "AND e.maximumStops = :maximumStops AND (:departner IS NULL OR e.departner = :departner)")
    fun findByFlightIdAndCriteria(
        @Param("flightId") flightId: String,
        @Param("source") source: String,
        @Param("destination") destination: String,
        @Param("flightDate") flightDate: LocalDate,
        @Param("maximumStops") maximumStops: Int,
        @Param("departner") departner: String?
    ): ElasticSearch?
}
