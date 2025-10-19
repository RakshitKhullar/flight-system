package com.reservation.service.reservation_system.config

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.test.context.TestPropertySource

@TestConfiguration
@TestPropertySource(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:testdb",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.cassandra.keyspace-name=test_keyspace",
        "spring.cassandra.contact-points=127.0.0.1",
        "spring.cassandra.port=9042",
        "logging.level.org.springframework.web=DEBUG"
    ]
)
class TestConfig {

    @Bean
    @Primary
    fun testMeterRegistry(): io.micrometer.core.instrument.MeterRegistry {
        return io.micrometer.core.instrument.simple.SimpleMeterRegistry()
    }
}
