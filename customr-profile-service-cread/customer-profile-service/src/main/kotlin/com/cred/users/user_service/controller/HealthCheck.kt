package com.cred.users.user_service.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


@RequestMapping("/api/v1")
@RestController
class HealthCheck {
    @GetMapping("/ping")
    suspend fun ping(): String {
        return "pong"
    }
}