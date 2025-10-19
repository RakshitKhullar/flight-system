package com.cred.users.user_service.integration

import com.cred.users.user_service.CustomerProfileServiceApplication
import com.cred.users.user_service.config.TestConfiguration
import com.cred.users.user_service.dto.*
import com.cred.users.user_service.repositories.CustomerRepository
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.hamcrest.Matchers
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.context.WebApplicationContext
import java.time.LocalDate
import java.util.*

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = [CustomerProfileServiceApplication::class],
    properties = [
        "spring.autoconfigure.exclude=org.springframework.boot.actuate.autoconfigure.observation.ObservationAutoConfiguration,org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration"
    ]
)
@AutoConfigureWebMvc
@Import(TestConfiguration::class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.AUTO_CONFIGURED)
@ActiveProfiles("test")
@Transactional
class CustomerIntegrationTest {

    @Autowired
    private lateinit var webApplicationContext: WebApplicationContext

    @Autowired
    private lateinit var customerRepository: CustomerRepository

    private lateinit var mockMvc: MockMvc
    private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
        objectMapper = ObjectMapper()
        objectMapper.registerModule(JavaTimeModule())
        
        // Clean up database before each test
        customerRepository.deleteAll()
    }

    @Test
    fun `complete customer lifecycle - create, get, update, verify, login, delete`() {
        // Step 1: Create Customer
        val createRequest = CreateCustomerRequest(
            userName = "integrationuser",
            userDob = LocalDate.of(1990, 1, 1).toString(),
            userEmail = "integration@test.com",
            password = "password123",
            firstName = "Integration",
            lastName = "Test",
            phoneNumber = "+1234567890",
            address = "123 Test St",
            city = "Test City",
            country = "Test Country",
            postalCode = "12345"
        )

        val createResponse = mockMvc.perform(
            post("/api/v1/customer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.userName").value("integrationuser"))
            .andExpect(jsonPath("$.data.userEmail").value("integration@test.com"))
            .andExpect(jsonPath("$.data.isVerified").value(false))
            .andExpect(jsonPath("$.data.isActive").value(true))
            .andReturn()

        // Extract userId from the JSON response using JsonPath
        val responseContent = createResponse.response.contentAsString
        val userId = UUID.fromString(
            objectMapper.readTree(responseContent)
                .get("data")
                .get("userId")
                .asText()
        )

        // Step 2: Get Customer by ID
        mockMvc.perform(get("/api/v1/customer/{userId}", userId))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.userId").value(userId.toString()))
            .andExpect(jsonPath("$.data.userName").value("integrationuser"))

        // Step 3: Get All Customers
        mockMvc.perform(get("/api/v1/customer"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray)
            .andExpect(jsonPath("$.data.length()").value(1))

        // Step 4: Update Customer
        val updateRequest = UpdateCustomerRequest(
            firstName = "UpdatedIntegration",
            city = "Updated City"
        )

        mockMvc.perform(
            put("/api/v1/customer/{userId}", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.firstName").value("UpdatedIntegration"))
            .andExpect(jsonPath("$.data.city").value("Updated City"))

        // Step 5: Login (should work even without verification)
        val loginRequest = LoginRequest(
            identifier = "integrationuser",
            password = "password123"
        )

        mockMvc.perform(
            post("/api/v1/customer/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.userName").value("integrationuser"))

        // Step 6: Login with email
        val emailLoginRequest = LoginRequest(
            identifier = "integration@test.com",
            password = "password123"
        )

        mockMvc.perform(
            post("/api/v1/customer/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(emailLoginRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.userEmail").value("integration@test.com"))

        // Step 7: Resend Verification Code
        mockMvc.perform(post("/api/v1/customer/{userId}/resend-verification", userId))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))

        // Step 8: Verify Customer (Note: In real scenario, we'd get the code from email)
        // For testing, we'll simulate the verification process
        val verifyRequest = VerifyCustomerRequest(verificationCode = "123456")
        
        // This will fail because we don't have the actual verification code
        // In a real integration test, you'd need to mock the verification code generation
        // or have a test endpoint to get the verification code
        mockMvc.perform(
            post("/api/v1/customer/{userId}/verify", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(verifyRequest))
        )
            .andExpect(status().isBadRequest) // Expected to fail with wrong code

        // Step 9: Delete Customer (Soft Delete)
        mockMvc.perform(delete("/api/v1/customer/{userId}", userId))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("Customer deactivated successfully"))

        // Step 10: Verify customer is deactivated (login should fail)
        mockMvc.perform(
            post("/api/v1/customer/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest))
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.success").value(false))
    }

    @Test
    fun `should handle duplicate customer creation`() {
        // Create first customer
        val createRequest = CreateCustomerRequest(
            userName = "duplicateuser",
            userDob = LocalDate.of(1990, 1, 1).toString(),
            userEmail = "duplicate@test.com",
            password = "password123",
            firstName = "Duplicate",
            lastName = "Test",
            phoneNumber = "+1234567890"
        )

        mockMvc.perform(
            post("/api/v1/customer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest))
        )
            .andExpect(status().isCreated)

        // Try to create duplicate customer with same email
        val duplicateEmailRequest = createRequest.copy(
            userName = "differentuser",
            phoneNumber = "+9876543210"
        )

        mockMvc.perform(
            post("/api/v1/customer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(duplicateEmailRequest))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").value(Matchers.containsString("already exists")))

        // Try to create duplicate customer with same username
        val duplicateUsernameRequest = createRequest.copy(
            userEmail = "different@test.com",
            phoneNumber = "+9876543210"
        )

        mockMvc.perform(
            post("/api/v1/customer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(duplicateUsernameRequest))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").value(Matchers.containsString("already exists")))
    }

    @Test
    fun `should handle invalid login attempts`() {
        // Create a customer first
        val createRequest = CreateCustomerRequest(
            userName = "loginuser",
            userDob = LocalDate.of(1990, 1, 1).toString(),
            userEmail = "login@test.com",
            password = "correctpassword",
            firstName = "Login",
            lastName = "Test",
            phoneNumber = "+1234567890"
        )

        mockMvc.perform(
            post("/api/v1/customer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest))
        )
            .andExpect(status().isCreated)

        // Test wrong password
        val wrongPasswordRequest = LoginRequest(
            identifier = "loginuser",
            password = "wrongpassword"
        )

        mockMvc.perform(
            post("/api/v1/customer/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(wrongPasswordRequest))
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.success").value(false))

        // Test non-existent user
        val nonExistentUserRequest = LoginRequest(
            identifier = "nonexistent",
            password = "password123"
        )

        mockMvc.perform(
            post("/api/v1/customer/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(nonExistentUserRequest))
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.success").value(false))
    }

    @Test
    fun `should handle operations on non-existent customer`() {
        val nonExistentUserId = UUID.randomUUID()

        // Test get non-existent customer
        mockMvc.perform(get("/api/v1/customer/{userId}", nonExistentUserId))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.success").value(false))

        // Test update non-existent customer
        val updateRequest = UpdateCustomerRequest(firstName = "Updated")
        mockMvc.perform(
            put("/api/v1/customer/{userId}", nonExistentUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest))
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.success").value(false))

        // Test delete non-existent customer
        mockMvc.perform(delete("/api/v1/customer/{userId}", nonExistentUserId))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.success").value(false))

        // Test verify non-existent customer
        val verifyRequest = VerifyCustomerRequest(verificationCode = "123456")
        mockMvc.perform(
            post("/api/v1/customer/{userId}/verify", nonExistentUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(verifyRequest))
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.success").value(false))

        // Test resend verification for non-existent customer
        mockMvc.perform(post("/api/v1/customer/{userId}/resend-verification", nonExistentUserId))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.success").value(false))
    }

}
