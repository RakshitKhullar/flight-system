package com.travel_search.travel_search_service.config

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.StringRedisSerializer
import redis.embedded.RedisServer

@TestConfiguration
class TestConfig {

    @Bean
    fun redisServer(): RedisServer {
        return RedisServer(6370)
    }

    @Bean
    @Primary
    fun testRedisConnectionFactory(): RedisConnectionFactory {
        return LettuceConnectionFactory("localhost", 6370)
    }

    @Bean
    @Primary
    fun testRedisTemplate(connectionFactory: RedisConnectionFactory): RedisTemplate<String, Any> {
        val template = RedisTemplate<String, Any>()
        template.connectionFactory = connectionFactory
        template.keySerializer = StringRedisSerializer()
        template.valueSerializer = GenericJackson2JsonRedisSerializer()
        template.afterPropertiesSet()
        return template
    }
}
