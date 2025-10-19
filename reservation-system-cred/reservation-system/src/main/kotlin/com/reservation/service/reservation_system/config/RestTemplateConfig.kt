package com.reservation.service.reservation_system.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.RestTemplate
import java.time.Duration

@Configuration
class RestTemplateConfig {
    
    @Bean
    fun restTemplate(): RestTemplate {
        val factory = SimpleClientHttpRequestFactory().apply {
            setConnectTimeout(Duration.ofSeconds(5).toMillis().toInt())
            setReadTimeout(Duration.ofSeconds(10).toMillis().toInt())
        }
        
        return RestTemplate(factory)
    }
}
