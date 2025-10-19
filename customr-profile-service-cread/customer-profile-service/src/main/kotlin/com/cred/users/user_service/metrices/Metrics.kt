package com.cred.users.user_service.metrices

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component("customerProfileMetrics")
class Metrics(
    private val meterRegistry: MeterRegistry
) {
    private val logger = LoggerFactory.getLogger(Metrics::class.java)

    // User service specific metrics
    val userRegistrationCounter: Counter = Counter.builder("cred_user_service_registration_counter")
        .description("Count of user registrations")
        .register(meterRegistry)

    val userLoginCounter: Counter = Counter.builder("cred_user_service_login_counter")
        .description("Count of user login attempts")
        .register(meterRegistry)

    val userProfileUpdateCounter: Counter = Counter.builder("cred_user_service_profile_update_counter")
        .description("Count of user profile updates")
        .register(meterRegistry)

    val apiErrorCounter: Counter = Counter.builder("cred_user_service_api_error_counter")
        .description("Count of API errors")
        .register(meterRegistry)

    val requestLatencyTimer: Timer = Timer.builder("cred_user_service_request_latency")
        .description("Request processing latency")
        .register(meterRegistry)

    // Metric functions
    fun incrementUserRegistration() {
        try {
            userRegistrationCounter.increment()
        } catch (e: Exception) {
            logger.error("Exception in user registration counter", e)
        }
    }

    fun incrementUserLogin() {
        try {
            userLoginCounter.increment()
        } catch (e: Exception) {
            logger.error("Exception in user login counter", e)
        }
    }

    fun incrementProfileUpdate() {
        try {
            userProfileUpdateCounter.increment()
        } catch (e: Exception) {
            logger.error("Exception in profile update counter", e)
        }
    }

    fun incrementApiError() {
        try {
            apiErrorCounter.increment()
        } catch (e: Exception) {
            logger.error("Exception in API error counter", e)
        }
    }

    fun recordRequestLatency(duration: Long, timeUnit: TimeUnit) {
        try {
            requestLatencyTimer.record(duration, timeUnit)
        } catch (e: Exception) {
            logger.error("Exception in request latency timer", e)
        }
    }
}