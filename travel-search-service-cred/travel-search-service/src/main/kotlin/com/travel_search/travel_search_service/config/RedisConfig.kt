package com.travel_search.travel_search_service.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.StringRedisSerializer
import java.time.Duration

@Configuration
@EnableCaching
class RedisConfig {

    @Bean
    fun redisTemplate(connectionFactory: RedisConnectionFactory): RedisTemplate<String, Any> {
        val template = RedisTemplate<String, Any>()
        template.connectionFactory = connectionFactory
        
        // Use String serializer for keys
        template.keySerializer = StringRedisSerializer()
        template.hashKeySerializer = StringRedisSerializer()
        
        // Use JSON serializer for values
        val objectMapper = ObjectMapper().apply {
            registerModule(KotlinModule.Builder().build())
            registerModule(JavaTimeModule())
        }
        val jsonSerializer = GenericJackson2JsonRedisSerializer(objectMapper)
        
        template.valueSerializer = jsonSerializer
        template.hashValueSerializer = jsonSerializer
        template.afterPropertiesSet()
        
        return template
    }

    @Bean
    fun cacheManager(connectionFactory: RedisConnectionFactory): CacheManager {
        val objectMapper = ObjectMapper().apply {
            registerModule(KotlinModule.Builder().build())
            registerModule(JavaTimeModule())
        }
        
        val redisCacheConfiguration = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(15)) // Default TTL of 15 minutes
            .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(GenericJackson2JsonRedisSerializer(objectMapper)))
            .disableCachingNullValues()

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(redisCacheConfiguration)
            .withCacheConfiguration("flightSearch", 
                redisCacheConfiguration.entryTtl(Duration.ofMinutes(10))) // Flight search cache for 10 minutes
            .withCacheConfiguration("elasticSearch", 
                redisCacheConfiguration.entryTtl(Duration.ofHours(1))) // Elastic search cache for 1 hour
            .build()
    }
}
