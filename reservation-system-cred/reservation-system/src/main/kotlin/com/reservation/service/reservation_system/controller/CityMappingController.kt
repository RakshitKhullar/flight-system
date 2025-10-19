package com.reservation.service.reservation_system.controller

import com.reservation.service.reservation_system.repository.entity.CityMapping
import com.reservation.service.reservation_system.service.CityMappingService
import com.reservation.service.reservation_system.metrics.MetricsService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/cities")
class CityMappingController(
    private val cityMappingService: CityMappingService,
    private val metricsService: MetricsService
) {

    @PostMapping
    fun createCityMapping(
        @RequestParam cityCode: String,
        @RequestParam cityName: String
    ): ResponseEntity<CityMapping> {
        return try {
            val cityMapping = cityMappingService.createCityMapping(cityCode, cityName)
            metricsService.incrementCityMappingCreated()
            ResponseEntity(cityMapping, HttpStatus.CREATED)
        } catch (e: IllegalArgumentException) {
            metricsService.incrementApiError()
            ResponseEntity.badRequest().build()
        }
    }

    @GetMapping
    fun getAllCities(): ResponseEntity<List<CityMapping>> {
        val cities = cityMappingService.getAllCities()
        return ResponseEntity.ok(cities)
    }

    @GetMapping("/code/{cityCode}")
    fun getCityByCode(@PathVariable cityCode: String): ResponseEntity<CityMapping> {
        return cityMappingService.getCityByCode(cityCode)
            ?.let { ResponseEntity.ok(it) }
            ?: ResponseEntity.notFound().build()
    }

    @GetMapping("/name/{cityName}")
    fun getCityByName(@PathVariable cityName: String): ResponseEntity<CityMapping> {
        return cityMappingService.getCityByName(cityName)
            ?.let { ResponseEntity.ok(it) }
            ?: ResponseEntity.notFound().build()
    }

    @GetMapping("/search")
    fun searchCitiesByName(@RequestParam cityName: String): ResponseEntity<List<CityMapping>> {
        val cities = cityMappingService.searchCitiesByName(cityName)
        return ResponseEntity.ok(cities)
    }

    @PutMapping("/{id}")
    fun updateCityMapping(
        @PathVariable id: Long,
        @RequestParam(required = false) cityCode: String?,
        @RequestParam(required = false) cityName: String?
    ): ResponseEntity<CityMapping> {
        return try {
            cityMappingService.updateCityMapping(id, cityCode, cityName)
                ?.let { ResponseEntity.ok(it) }
                ?: ResponseEntity.notFound().build()
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().build()
        }
    }

    @DeleteMapping("/{id}")
    fun deleteCityMapping(@PathVariable id: Long): ResponseEntity<Void> {
        return if (cityMappingService.deleteCityMapping(id)) {
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @PostMapping("/initialize")
    fun initializeDefaultCities(): ResponseEntity<String> {
        cityMappingService.initializeDefaultCities()
        return ResponseEntity.ok("Default cities initialized successfully")
    }
}
