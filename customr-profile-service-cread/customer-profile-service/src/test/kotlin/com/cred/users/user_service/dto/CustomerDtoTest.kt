package com.cred.users.user_service.dto

import jakarta.validation.Validation
import jakarta.validation.Validator
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class CustomerDtoTest {

    private lateinit var validator: Validator

    @BeforeEach
    fun setUp() {
        val factory = Validation.buildDefaultValidatorFactory()
        validator = factory.validator
    }

    @Test
    fun `CreateCustomerRequest should be valid with all required fields`() {
        // Given
        val request = CreateCustomerRequest(
            userName = "testuser",
            userDob = LocalDate.of(1990, 1, 1).toString(),
            userEmail = "test@example.com",
            password = "password123",
            firstName = "John",
            lastName = "Doe",
            phoneNumber = "+1234567890",
            address = "123 Test St",
            city = "Test City",
            country = "Test Country",
            postalCode = "12345"
        )

        // When
        val violations = validator.validate(request)

        // Then
        assertTrue(violations.isEmpty(), "Should have no validation errors")
    }

    @Test
    fun `CreateCustomerRequest should be invalid with blank required fields`() {
        // Given
        val request = CreateCustomerRequest(
            userName = "", // Blank
            userDob = LocalDate.of(1990, 1, 1).toString(),
            userEmail = "", // Blank
            password = "", // Blank
            firstName = "", // Blank
            lastName = "", // Blank
            phoneNumber = "", // Blank
            address = null,
            city = null,
            country = null,
            postalCode = null
        )

        // When
        val violations = validator.validate(request)

        // Then
        assertFalse(violations.isEmpty(), "Should have validation errors")
        assertTrue(violations.size >= 5, "Should have at least 5 validation errors")
        
        val violationMessages = violations.map { it.message }
        assertTrue(violationMessages.any { it.contains("blank") || it.contains("empty") })
    }

    @Test
    fun `CreateCustomerRequest should be invalid with invalid email format`() {
        // Given
        val request = CreateCustomerRequest(
            userName = "testuser",
            userDob = LocalDate.of(1990, 1, 1).toString(),
            userEmail = "invalid-email-format", // Invalid email
            password = "password123",
            firstName = "John",
            lastName = "Doe",
            phoneNumber = "+1234567890"
        )

        // When
        val violations = validator.validate(request)

        // Then
        assertFalse(violations.isEmpty(), "Should have validation errors")
        val emailViolations = violations.filter { it.propertyPath.toString() == "userEmail" }
        assertTrue(emailViolations.isNotEmpty(), "Should have email validation error")
    }

    @Test
    fun `UpdateCustomerRequest should be valid with all null fields`() {
        // Given
        val request = UpdateCustomerRequest(
            userName = null,
            userDob = null,
            userEmail = null,
            firstName = null,
            lastName = null,
            phoneNumber = null,
            address = null,
            city = null,
            country = null,
            postalCode = null
        )

        // When
        val violations = validator.validate(request)

        // Then
        assertTrue(violations.isEmpty(), "Should have no validation errors for null fields")
    }

    @Test
    fun `UpdateCustomerRequest should be valid with some fields populated`() {
        // Given
        val request = UpdateCustomerRequest(
            userName = "updateduser",
            userEmail = "updated@example.com",
            firstName = "UpdatedJohn",
            lastName = null,
            phoneNumber = null,
            address = "Updated Address",
            city = null,
            country = null,
            postalCode = null
        )

        // When
        val violations = validator.validate(request)

        // Then
        assertTrue(violations.isEmpty(), "Should have no validation errors")
    }

    @Test
    fun `LoginRequest should be valid with username and password`() {
        // Given
        val request = LoginRequest(
            identifier = "testuser",
            password = "password123"
        )

        // When
        val violations = validator.validate(request)

        // Then
        assertTrue(violations.isEmpty(), "Should have no validation errors")
    }

    @Test
    fun `LoginRequest should be valid with email and password`() {
        // Given
        val request = LoginRequest(
            identifier = "test@example.com",
            password = "password123"
        )

        // When
        val violations = validator.validate(request)

        // Then
        assertTrue(violations.isEmpty(), "Should have no validation errors")
    }

    @Test
    fun `LoginRequest should be invalid with blank fields`() {
        // Given
        val request = LoginRequest(
            identifier = "", // Blank
            password = "" // Blank
        )

        // When
        val violations = validator.validate(request)

        // Then
        assertFalse(violations.isEmpty(), "Should have validation errors")
        assertEquals(2, violations.size, "Should have exactly 2 validation errors")
    }

    @Test
    fun `VerifyCustomerRequest should be valid with verification code`() {
        // Given
        val request = VerifyCustomerRequest(verificationCode = "123456")

        // When
        val violations = validator.validate(request)

        // Then
        assertTrue(violations.isEmpty(), "Should have no validation errors")
    }

    @Test
    fun `VerifyCustomerRequest should be invalid with blank verification code`() {
        // Given
        val request = VerifyCustomerRequest(verificationCode = "")

        // When
        val violations = validator.validate(request)

        // Then
        assertFalse(violations.isEmpty(), "Should have validation errors")
        assertEquals(1, violations.size, "Should have exactly 1 validation error")
    }

    @Test
    fun `CustomerResponse should contain all required fields`() {
        // Given
        val userId = UUID.randomUUID()
        val now = LocalDateTime.now()
        
        val response = CustomerResponse(
            userId = userId,
            userName = "testuser",
            userDob = LocalDate.of(1990, 1, 1).toString(),
            userEmail = "test@example.com",
            firstName = "John",
            lastName = "Doe",
            phoneNumber = "+1234567890",
            address = "123 Test St",
            city = "Test City",
            country = "Test Country",
            postalCode = "12345",
            isVerified = false,
            isActive = true,
            createdAt = now,
            updatedAt = now
        )

        // Then
        assertNotNull(response.userId)
        assertEquals("testuser", response.userName)
        assertEquals("test@example.com", response.userEmail)
        assertEquals("John", response.firstName)
        assertEquals("Doe", response.lastName)
        assertEquals("+1234567890", response.phoneNumber)
        assertFalse(response.isVerified)
        assertTrue(response.isActive)
        assertNotNull(response.createdAt)
        assertNotNull(response.updatedAt)
    }

    @Test
    fun `ApiResponse should handle different data types`() {
        // Given
        val stringResponse = ApiResponse(
            success = true,
            message = "Success",
            data = "String data"
        )
        
        val customerResponse = ApiResponse(
            success = true,
            message = "Customer retrieved",
            data = CustomerResponse(
                userId = UUID.randomUUID(),
                userName = "test",
                userDob = LocalDate.of(1990, 1, 1).toString(),
                userEmail = "test@example.com",
                firstName = "John",
                lastName = "Doe",
                phoneNumber = "+1234567890",
                address = null,
                city = null,
                country = null,
                postalCode = null,
                isVerified = false,
                isActive = true,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )
        )
        
        val errorResponse = ApiResponse<String>(
            success = false,
            message = "Error occurred",
            data = null
        )

        // Then
        assertTrue(stringResponse.success)
        assertEquals("String data", stringResponse.data)
        assertNotNull(stringResponse.timestamp)
        
        assertTrue(customerResponse.success)
        assertNotNull(customerResponse.data)
        assertEquals("test", customerResponse.data!!.userName)
        
        assertFalse(errorResponse.success)
        assertNull(errorResponse.data)
        assertEquals("Error occurred", errorResponse.message)
    }

    @Test
    fun `CustomerVerificationResponse should contain verification details`() {
        // Given
        val userId = UUID.randomUUID()
        val response = CustomerVerificationResponse(
            userId = userId,
            isVerified = true,
            message = "Customer verified successfully"
        )

        // Then
        assertEquals(userId, response.userId)
        assertTrue(response.isVerified)
        assertEquals("Customer verified successfully", response.message)
    }

    @Test
    fun `DTOs should handle edge cases properly`() {
        // Test with very long strings
        val longString = "a".repeat(1000)
        
        val request = CreateCustomerRequest(
            userName = "user",
            userDob = LocalDate.of(1990, 1, 1).toString(),
            userEmail = "test@example.com",
            password = "password",
            firstName = longString,
            lastName = longString,
            phoneNumber = "+1234567890",
            address = longString,
            city = longString,
            country = longString,
            postalCode = longString
        )

        // Should not throw exceptions when creating the object
        assertNotNull(request)
        assertEquals(longString, request.firstName)
        assertEquals(longString, request.address)
    }

    @Test
    fun `DTOs should handle special characters properly`() {
        // Given
        val specialChars = "!@#$%^&*()_+-=[]{}|;:,.<>?"
        val unicodeChars = "测试用户名密码🔐"
        
        val request = CreateCustomerRequest(
            userName = "user_test",
            userDob = LocalDate.of(1990, 1, 1).toString(),
            userEmail = "test@example.com",
            password = specialChars,
            firstName = unicodeChars,
            lastName = "Test",
            phoneNumber = "+1234567890"
        )

        // When
        val violations = validator.validate(request)

        // Then
        assertTrue(violations.isEmpty(), "Should handle special characters without validation errors")
        assertEquals(specialChars, request.password)
        assertEquals(unicodeChars, request.firstName)
    }
}
