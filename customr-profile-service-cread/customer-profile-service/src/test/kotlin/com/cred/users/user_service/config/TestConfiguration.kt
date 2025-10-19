package com.cred.users.user_service.config

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary

@TestConfiguration
class TestConfiguration {
    
    @Bean
    @Primary
    fun meterRegistry(): MeterRegistry {
        return SimpleMeterRegistry()
    }
}
