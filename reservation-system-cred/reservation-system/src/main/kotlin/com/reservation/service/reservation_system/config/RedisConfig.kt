package com.reservation.service.reservation_system.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.StringRedisSerializer
import redis.clients.jedis.JedisPoolConfig

@Configuration
class RedisConfig {

    @Value("\${spring.redis.host:localhost}")
    private lateinit var redisHost: String

    @Value("\${spring.redis.port:6379}")
    private var redisPort: Int = 6379

    @Value("\${spring.redis.password:}")
    private lateinit var redisPassword: String

    @Value("\${spring.redis.timeout:2000}")
    private var timeout: Int = 2000

    @Value("\${spring.redis.jedis.pool.max-active:8}")
    private var maxActive: Int = 8

    @Value("\${spring.redis.jedis.pool.max-idle:8}")
    private var maxIdle: Int = 8

    @Value("\${spring.redis.jedis.pool.min-idle:0}")
    private var minIdle: Int = 0

    @Bean
    fun jedisConnectionFactory(): JedisConnectionFactory {
        val redisStandaloneConfiguration = RedisStandaloneConfiguration().apply {
            hostName = redisHost
            port = redisPort
            if (redisPassword.isNotBlank()) {
                setPassword(redisPassword)
            }
        }

        val jedisPoolConfig = JedisPoolConfig().apply {
            maxTotal = maxActive
            maxIdle = this@RedisConfig.maxIdle
            minIdle = this@RedisConfig.minIdle
            testOnBorrow = true
            testOnReturn = true
            testWhileIdle = true
        }

        return JedisConnectionFactory(redisStandaloneConfiguration).apply {
            setPoolConfig(jedisPoolConfig)
            setTimeout(timeout)
        }
    }

    @Bean
    fun redisTemplate(connectionFactory: RedisConnectionFactory): RedisTemplate<String, Any> {
        return RedisTemplate<String, Any>().apply {
            setConnectionFactory(connectionFactory)
            
            // Use String serializer for keys
            keySerializer = StringRedisSerializer()
            hashKeySerializer = StringRedisSerializer()
            
            // Use JSON serializer for values
            valueSerializer = GenericJackson2JsonRedisSerializer()
            hashValueSerializer = GenericJackson2JsonRedisSerializer()
            
            // Enable transaction support
            setEnableTransactionSupport(true)
            
            afterPropertiesSet()
        }
    }

    @Bean
    fun stringRedisTemplate(connectionFactory: RedisConnectionFactory): RedisTemplate<String, String> {
        return RedisTemplate<String, String>().apply {
            setConnectionFactory(connectionFactory)
            
            // Use String serializer for both keys and values
            keySerializer = StringRedisSerializer()
            valueSerializer = StringRedisSerializer()
            hashKeySerializer = StringRedisSerializer()
            hashValueSerializer = StringRedisSerializer()
            
            afterPropertiesSet()
        }
    }
}
