package com.reservation.service.reservation_system.repository

import com.reservation.service.reservation_system.repository.entity.CityMapping
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface CityMappingRepository : JpaRepository<CityMapping, Long> {
    
    fun findByCityCode(cityCode: String): CityMapping?
    
    fun findByCityName(cityName: String): CityMapping?
    
    fun findByCityNameContainingIgnoreCase(cityName: String): List<CityMapping>
    
    fun existsByCityCode(cityCode: String): Boolean
    
    fun existsByCityName(cityName: String): Boolean
}
