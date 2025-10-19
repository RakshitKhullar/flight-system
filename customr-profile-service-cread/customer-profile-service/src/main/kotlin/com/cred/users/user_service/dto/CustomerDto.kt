package com.cred.users.user_service.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.time.LocalDateTime
import java.util.*

@JsonIgnoreProperties(ignoreUnknown = true)
data class CreateCustomerRequest(
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    val userName: String,

    @NotBlank(message = "Date of birth is required")
    val userDob: String, // Format: YYYY-MM-DD

    @Email(message = "Invalid email format")
    @NotBlank(message = "Email is required")
    val userEmail: String,

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    val password: String,

    @NotBlank(message = "First name is required")
    val firstName: String,

    @NotBlank(message = "Last name is required")
    val lastName: String,

    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number format")
    @NotBlank(message = "Phone number is required")
    val phoneNumber: String,

    val address: String? = null,
    val city: String? = null,
    val country: String? = null,
    val postalCode: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class UpdateCustomerRequest(
    val userName: String? = null,
    val userDob: String? = null,
    @Email(message = "Invalid email format")
    val userEmail: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number format")
    val phoneNumber: String? = null,
    val address: String? = null,
    val city: String? = null,
    val country: String? = null,
    val postalCode: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CustomerResponse(
    val userId: UUID, // This is what we expose to users instead of internal ID
    val userName: String,
    val userDob: String,
    val userEmail: String,
    val firstName: String,
    val lastName: String,
    val phoneNumber: String,
    val address: String? = null,
    val city: String? = null,
    val country: String? = null,
    val postalCode: String? = null,
    val isVerified: Boolean,
    val isActive: Boolean,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class VerifyCustomerRequest(
    @NotBlank(message = "Verification code is required")
    val verificationCode: String
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class LoginRequest(
    @NotBlank(message = "Username or email is required")
    val identifier: String, // Can be username or email
    
    @NotBlank(message = "Password is required")
    val password: String
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ApiResponse<T>(
    val success: Boolean,
    val message: String,
    val data: T? = null,
    val timestamp: LocalDateTime = LocalDateTime.now()
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CustomerVerificationResponse(
    val userId: UUID, // Use UUID instead of internal customer ID
    val isVerified: Boolean,
    val message: String
)
