package com.travel_search.travel_search_service.controller

import com.travel_search.travel_search_service.service.CacheService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/cache")
class CacheController(
    private val cacheService: CacheService
) {

    @GetMapping("/info")
    fun getCacheInfo(): ResponseEntity<Map<String, Any>> {
        return ResponseEntity.ok(cacheService.getCacheInfo())
    }

    @DeleteMapping("/flight-search")
    fun evictFlightSearchCache(): ResponseEntity<Map<String, String>> {
        cacheService.evictFlightSearchCache()
        return ResponseEntity.ok(mapOf("message" to "Flight search cache cleared successfully"))
    }

    @DeleteMapping("/all")
    fun clearAllCache(): ResponseEntity<Map<String, String>> {
        cacheService.clearAllCache()
        return ResponseEntity.ok(mapOf("message" to "All cache cleared successfully"))
    }
}
