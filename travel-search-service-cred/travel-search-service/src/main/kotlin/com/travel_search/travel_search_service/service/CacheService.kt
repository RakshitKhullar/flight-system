package com.travel_search.travel_search_service.service

import com.travel_search.travel_search_service.common.utils.CacheKeyGenerator
import com.travel_search.travel_search_service.dto.FlightSearchRequest
import com.travel_search.travel_search_service.dto.FlightSearchResponse
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class CacheService(
    private val redisTemplate: RedisTemplate<String, Any>
) {
    
    fun getCachedFlightSearch(request: FlightSearchRequest): FlightSearchResponse? {
        val key = CacheKeyGenerator.generateFlightSearchKey(request)
        return redisTemplate.opsForValue().get(key) as? FlightSearchResponse
    }
    
    fun cacheFlightSearch(request: FlightSearchRequest, response: FlightSearchResponse) {
        val key = CacheKeyGenerator.generateFlightSearchKey(request)
        redisTemplate.opsForValue().set(key, response, 10, TimeUnit.MINUTES)
    }
    
    fun evictFlightSearchCache(pattern: String = "flight_search:*") {
        val keys = redisTemplate.keys(pattern)
        if (keys.isNotEmpty()) {
            redisTemplate.delete(keys)
        }
    }
    
    fun getCacheInfo(): Map<String, Any> {
        val flightSearchKeys = redisTemplate.keys("flight_search:*")
        val elasticSearchKeys = redisTemplate.keys("elastic_search:*")
        
        return mapOf(
            "flightSearchCacheCount" to flightSearchKeys.size,
            "elasticSearchCacheCount" to elasticSearchKeys.size,
            "totalCacheKeys" to (flightSearchKeys.size + elasticSearchKeys.size)
        )
    }
    
    fun clearAllCache() {
        redisTemplate.connectionFactory?.connection?.flushAll()
    }
}
