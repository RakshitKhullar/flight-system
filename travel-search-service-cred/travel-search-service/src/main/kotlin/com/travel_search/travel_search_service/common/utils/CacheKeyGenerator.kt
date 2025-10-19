package com.travel_search.travel_search_service.common.utils

import com.travel_search.travel_search_service.dto.FlightSearchRequest
import java.security.MessageDigest

object CacheKeyGenerator {
    
    fun generateFlightSearchKey(request: FlightSearchRequest): String {
        val keyData = "${request.src}-${request.destination}-${request.startDate}-" +
                     "${request.departner.sorted().joinToString(",")}-" +
                     "${request.maximumStops.sorted().joinToString(",")}-" +
                     "${request.sortBy}"
        
        return "flight_search:${hashKey(keyData)}"
    }
    
    fun generateElasticSearchKey(flightId: String): String {
        return "elastic_search:$flightId"
    }
    
    private fun hashKey(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(input.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
}
