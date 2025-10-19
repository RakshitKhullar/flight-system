package com.cred.users.user_service.controller

import com.cred.users.user_service.metrices.Metrics
import com.cred.users.user_service.dto.*
import com.cred.users.user_service.service.CustomerServiceInterface
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.mockk.*
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@ExtendWith(MockKExtension::class)
class CustomerProfileTest {

    private lateinit var mockMvc: MockMvc
    private lateinit var customerService: CustomerServiceInterface
    private lateinit var metrics: Metrics
    private lateinit var customerProfile: CustomerProfile
    private lateinit var objectMapper: ObjectMapper

    private val testUserId = UUID.randomUUID()
    private val testCustomerResponse = CustomerResponse(
        userId = testUserId,
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
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now()
    )

    @BeforeEach
    fun setUp() {
        customerService = mockk()
        metrics = mockk(relaxed = true)
        customerProfile = CustomerProfile(customerService, metrics)
        
        mockMvc = MockMvcBuilders.standaloneSetup(customerProfile).build()
        
        objectMapper = ObjectMapper()
        objectMapper.registerModule(JavaTimeModule())
    }

    @Test
    fun `createCustomer should return 201 when customer created successfully`() {
        // Given
        val createRequest = CreateCustomerRequest(
            userName = "newuser",
            userDob = LocalDate.of(1995, 5, 15).toString(),
            userEmail = "newuser@example.com",
            password = "password123",
            firstName = "Jane",
            lastName = "Smith",
            phoneNumber = "+9876543210",
            address = "456 New St",
            city = "New City",
            country = "New Country",
            postalCode = "54321"
        )

        every { customerService.createCustomer(any()) } returns testCustomerResponse

        // When & Then
        mockMvc.perform(
            post("/api/v1/customer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("Customer created successfully. Verification code sent to email."))
            .andExpect(jsonPath("$.data.userName").value(testCustomerResponse.userName))
            .andExpect(jsonPath("$.data.userEmail").value(testCustomerResponse.userEmail))

        verify { customerService.createCustomer(any()) }
    }

    @Test
    fun `createCustomer should return 400 when validation fails`() {
        // Given
        val invalidRequest = CreateCustomerRequest(
            userName = "", // Invalid empty username
            userDob = LocalDate.of(1995, 5, 15).toString(),
            userEmail = "invalid-email", // Invalid email format
            password = "",
            firstName = "Jane",
            lastName = "Smith",
            phoneNumber = "+9876543210"
        )

        // When & Then
        mockMvc.perform(
            post("/api/v1/customer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest))
        )
            .andExpect(status().isBadRequest)

        verify(exactly = 0) { customerService.createCustomer(any()) }
    }

    @Test
    fun `createCustomer should return 400 when customer already exists`() {
        // Given
        val createRequest = CreateCustomerRequest(
            userName = "existinguser",
            userDob = LocalDate.of(1995, 5, 15).toString(),
            userEmail = "existing@example.com",
            password = "password123",
            firstName = "Jane",
            lastName = "Smith",
            phoneNumber = "+9876543210"
        )

        every { customerService.createCustomer(any()) } throws IllegalArgumentException("Customer already exists")

        // When & Then
        mockMvc.perform(
            post("/api/v1/customer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").value("Customer already exists"))

        verify { customerService.createCustomer(any()) }
    }

    @Test
    fun `getCustomerByUserId should return 200 when customer found`() {
        // Given
        every { customerService.getCustomerByUserId(testUserId) } returns testCustomerResponse

        // When & Then
        mockMvc.perform(get("/api/v1/customer/{userId}", testUserId))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("Get customer by userId: $testUserId completed successfully"))
            .andExpect(jsonPath("$.data.userId").value(testUserId.toString()))
            .andExpect(jsonPath("$.data.userName").value(testCustomerResponse.userName))
            .andExpect(jsonPath("$.data.userEmail").value(testCustomerResponse.userEmail))

        verify { customerService.getCustomerByUserId(testUserId) }
    }

    @Test
    fun `getCustomerByUserId should return 404 when customer not found`() {
        // Given
        every { customerService.getCustomerByUserId(testUserId) } throws NoSuchElementException("Customer not found")

        // When & Then
        mockMvc.perform(get("/api/v1/customer/{userId}", testUserId))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").value("Customer not found"))

        verify { customerService.getCustomerByUserId(testUserId) }
    }

    @Test
    fun `getAllCustomers should return 200 with list of customers`() {
        // Given
        val customersList = listOf(testCustomerResponse, testCustomerResponse.copy(userId = UUID.randomUUID()))
        every { customerService.getAllCustomers() } returns customersList

        // When & Then
        mockMvc.perform(get("/api/v1/customer"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray)
            .andExpect(jsonPath("$.data.length()").value(2))

        verify { customerService.getAllCustomers() }
    }

    @Test
    fun `updateCustomer should return 200 when customer updated successfully`() {
        // Given
        val updateRequest = UpdateCustomerRequest(
            firstName = "UpdatedJohn",
            lastName = "UpdatedDoe"
        )
        
        val updatedCustomer = testCustomerResponse.copy(
            firstName = "UpdatedJohn",
            lastName = "UpdatedDoe"
        )

        every { customerService.updateCustomer(testUserId, any()) } returns updatedCustomer

        // When & Then
        mockMvc.perform(
            put("/api/v1/customer/{userId}", testUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.firstName").value("UpdatedJohn"))
            .andExpect(jsonPath("$.data.lastName").value("UpdatedDoe"))

        verify { customerService.updateCustomer(testUserId, any()) }
    }

    @Test
    fun `updateCustomer should return 404 when customer not found`() {
        // Given
        val updateRequest = UpdateCustomerRequest(firstName = "Updated")
        every { customerService.updateCustomer(testUserId, any()) } throws NoSuchElementException("Customer not found")

        // When & Then
        mockMvc.perform(
            put("/api/v1/customer/{userId}", testUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest))
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").value("Customer not found"))

        verify { customerService.updateCustomer(testUserId, any()) }
    }

    @Test
    fun `deleteCustomer should return 200 when customer deleted successfully`() {
        // Given
        every { customerService.deleteCustomer(testUserId) } just Runs

        // When & Then
        mockMvc.perform(delete("/api/v1/customer/{userId}", testUserId))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("Customer deactivated successfully"))
            .andExpect(jsonPath("$.data").value("Customer with userId $testUserId has been deactivated"))

        verify { customerService.deleteCustomer(testUserId) }
    }

    @Test
    fun `deleteCustomer should return 404 when customer not found`() {
        // Given
        every { customerService.deleteCustomer(testUserId) } throws NoSuchElementException("Customer not found")

        // When & Then
        mockMvc.perform(delete("/api/v1/customer/{userId}", testUserId))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").value("Customer not found"))

        verify { customerService.deleteCustomer(testUserId) }
    }

    @Test
    fun `loginCustomer should return 200 when login successful`() {
        // Given
        val loginRequest = LoginRequest(
            identifier = "testuser",
            password = "password123"
        )

        every { customerService.loginCustomer(any()) } returns testCustomerResponse

        // When & Then
        mockMvc.perform(
            post("/api/v1/customer/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("Customer login for: ${loginRequest.identifier} successful"))
            .andExpect(jsonPath("$.data.userName").value(testCustomerResponse.userName))

        verify { customerService.loginCustomer(any()) }
    }

    @Test
    fun `loginCustomer should return 401 when credentials are invalid`() {
        // Given
        val loginRequest = LoginRequest(
            identifier = "testuser",
            password = "wrongpassword"
        )

        every { customerService.loginCustomer(any()) } throws IllegalArgumentException("Invalid credentials")

        // When & Then
        mockMvc.perform(
            post("/api/v1/customer/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest))
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").value("Invalid credentials"))

        verify { customerService.loginCustomer(any()) }
    }

    @Test
    fun `verifyCustomer should return 200 when verification successful`() {
        // Given
        val verifyRequest = VerifyCustomerRequest(verificationCode = "123456")
        val verificationResponse = CustomerVerificationResponse(
            userId = testUserId,
            isVerified = true,
            message = "Customer verified successfully"
        )

        every { customerService.verifyCustomer(testUserId, any()) } returns verificationResponse

        // When & Then
        mockMvc.perform(
            post("/api/v1/customer/{userId}/verify", testUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(verifyRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.userId").value(testUserId.toString()))
            .andExpect(jsonPath("$.data.isVerified").value(true))

        verify { customerService.verifyCustomer(testUserId, any()) }
    }

    @Test
    fun `verifyCustomer should return 400 when verification code is invalid`() {
        // Given
        val verifyRequest = VerifyCustomerRequest(verificationCode = "wrong123")
        every { customerService.verifyCustomer(testUserId, any()) } throws IllegalArgumentException("Invalid verification code")

        // When & Then
        mockMvc.perform(
            post("/api/v1/customer/{userId}/verify", testUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(verifyRequest))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").value("Invalid verification code"))

        verify { customerService.verifyCustomer(testUserId, any()) }
    }

    @Test
    fun `resendVerificationCode should return 200 when code resent successfully`() {
        // Given
        every { customerService.resendVerificationCode(testUserId) } returns "Verification code sent to test@example.com"

        // When & Then
        mockMvc.perform(post("/api/v1/customer/{userId}/resend-verification", testUserId))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("Verification code resent successfully"))
            .andExpect(jsonPath("$.data").value("Verification code resent successfully"))

        verify { customerService.resendVerificationCode(testUserId) }
    }

    @Test
    fun `resendVerificationCode should return 400 when customer already verified`() {
        // Given
        every { customerService.resendVerificationCode(testUserId) } throws IllegalStateException("Customer is already verified")

        // When & Then
        mockMvc.perform(post("/api/v1/customer/{userId}/resend-verification", testUserId))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").value("Customer is already verified"))

        verify { customerService.resendVerificationCode(testUserId) }
    }
}
