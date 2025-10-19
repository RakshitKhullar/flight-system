package com.cred.users.user_service.repositories.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "customers")
data class Customer(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "user_id", unique = true, nullable = false, updatable = false)
    val userId: UUID = UUID.randomUUID(),

    @Column(unique = true, nullable = false)
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    val userName: String,

    @Column(nullable = false)
    @NotBlank(message = "Date of birth is required")
    val userDob: String, // Format: YYYY-MM-DD

    @Column(name = "user_email", unique = true, nullable = false)
    @Email(message = "Invalid email format")
    @NotBlank(message = "Email is required")
    val userEmail: String,

    @Column(nullable = false)
    @NotBlank(message = "Password is required")
    val password: String, // This will be encrypted using AES256

    @Column(nullable = false)
    @NotBlank(message = "First name is required")
    val firstName: String,

    @Column(nullable = false)
    @NotBlank(message = "Last name is required")
    val lastName: String,

    @Column(unique = true, nullable = false)
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number format")
    @NotBlank(message = "Phone number is required")
    val phoneNumber: String,

    @Column
    val address: String? = null,

    @Column
    val city: String? = null,

    @Column
    val country: String? = null,

    @Column
    val postalCode: String? = null,

    @Column(nullable = false)
    val isVerified: Boolean = false,

    @Column(nullable = false)
    val isActive: Boolean = true,

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now()
)