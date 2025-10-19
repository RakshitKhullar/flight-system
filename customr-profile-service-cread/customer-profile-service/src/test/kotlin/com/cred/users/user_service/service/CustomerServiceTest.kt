package com.cred.users.user_service.service

import com.cred.users.user_service.metrices.Metrics
import com.cred.users.user_service.dto.*
import com.cred.users.user_service.repositories.CustomerRepository
import com.cred.users.user_service.repositories.entity.Customer
import com.cred.users.user_service.utils.PasswordUtils
import io.mockk.*
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@ExtendWith(MockKExtension::class)
class CustomerServiceTest {

    private lateinit var customerService: CustomerService
    private lateinit var customerRepository: CustomerRepository
    private lateinit var metrics: Metrics
    private lateinit var passwordUtils: PasswordUtils

    private val testUserId = UUID.randomUUID()
    private val testCustomer = Customer(
        id = 1L,
        userId = testUserId,
        userName = "testuser",
        userDob = LocalDate.of(1990, 1, 1).toString(),
        userEmail = "test@example.com",
        password = "encrypted_password",
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
        clearAllMocks()
        customerRepository = mockk()
        metrics = mockk(relaxed = true)
        passwordUtils = mockk()
        customerService = CustomerService(customerRepository, metrics, passwordUtils)
    }

    @Test
    fun `createCustomer should create new customer successfully`() {
        // Given
        val request = CreateCustomerRequest(
            userName = "newuser",
            userDob = LocalDate.of(1995, 5, 15).toString(),
            userEmail = "newuser@example.com",
            password = "password123",
            firstName = "John",
            lastName = "Doe",
            phoneNumber = "+9876543210",
            address = "456 New St",
            city = "New City",
            country = "New Country",
            postalCode = "54321"
        )

        every { customerRepository.existsByUserEmail(request.userEmail) } returns false
        every { customerRepository.existsByUserName(request.userName) } returns false
        every { customerRepository.existsByPhoneNumber(request.phoneNumber) } returns false
        every { passwordUtils.encryptPassword(request.password) } returns "encrypted_password"
        every { customerRepository.save(any<Customer>()) } returns testCustomer.copy(
            userName = request.userName,
            userEmail = request.userEmail
        )

        // When
        val result = customerService.createCustomer(request)

        // Then
        assertNotNull(result)
        assertEquals(request.userName, result.userName)
        assertEquals(request.userEmail, result.userEmail)
        assertEquals(request.firstName, result.firstName)
        assertEquals(request.lastName, result.lastName)
        
        verify { customerRepository.existsByUserEmail(request.userEmail) }
        verify { customerRepository.existsByUserName(request.userName) }
        verify { customerRepository.existsByPhoneNumber(request.phoneNumber) }
        verify { passwordUtils.encryptPassword(request.password) }
        verify { customerRepository.save(any<Customer>()) }
        verify { metrics.incrementUserRegistration() }
    }

    @Test
    fun `createCustomer should throw exception when email already exists`() {
        // Given
        val request = CreateCustomerRequest(
            userName = "newuser",
            userDob = LocalDate.of(1995, 5, 15).toString(),
            userEmail = "existing@example.com",
            password = "password123",
            firstName = "Jane",
            lastName = "Smith",
            phoneNumber = "+9876543210"
        )

        every { customerRepository.existsByUserEmail(request.userEmail) } returns true

        // When & Then
        val exception = assertThrows<IllegalArgumentException> {
            customerService.createCustomer(request)
        }
        
        assertEquals("Customer with email ${request.userEmail} already exists", exception.message)
        verify { customerRepository.existsByUserEmail(request.userEmail) }
        verify(exactly = 0) { customerRepository.save(any<Customer>()) }
    }

    @Test
    fun `createCustomer should throw exception when username already exists`() {
        // Given
        val request = CreateCustomerRequest(
            userName = "existinguser",
            userDob = LocalDate.of(1995, 5, 15).toString(),
            userEmail = "new@example.com",
            password = "password123",
            firstName = "Jane",
            lastName = "Smith",
            phoneNumber = "+9876543210"
        )

        every { customerRepository.existsByUserEmail(request.userEmail) } returns false
        every { customerRepository.existsByUserName(request.userName) } returns true

        // When & Then
        val exception = assertThrows<IllegalArgumentException> {
            customerService.createCustomer(request)
        }
        
        assertEquals("Customer with username ${request.userName} already exists", exception.message)
        verify { customerRepository.existsByUserName(request.userName) }
        verify(exactly = 0) { customerRepository.save(any<Customer>()) }
    }

    @Test
    fun `getCustomerByUserId should return customer when found`() {
        // Given
        every { customerRepository.findByUserId(testUserId) } returns Optional.of(testCustomer)

        // When
        val result = customerService.getCustomerByUserId(testUserId)

        // Then
        assertNotNull(result)
        assertEquals(testCustomer.userId, result.userId)
        assertEquals(testCustomer.userName, result.userName)
        assertEquals(testCustomer.userEmail, result.userEmail)
        
        verify { customerRepository.findByUserId(testUserId) }
    }

    @Test
    fun `getCustomerByUserId should throw exception when customer not found`() {
        // Given
        every { customerRepository.findByUserId(testUserId) } returns Optional.empty()

        // When & Then
        val exception = assertThrows<NoSuchElementException> {
            customerService.getCustomerByUserId(testUserId)
        }
        
        assertEquals("Customer not found with userId: $testUserId", exception.message)
        verify { customerRepository.findByUserId(testUserId) }
    }

    @Test
    fun `getAllCustomers should return list of customers`() {
        // Given
        val customers = listOf(testCustomer, testCustomer.copy(id = 2L, userId = UUID.randomUUID()))
        every { customerRepository.findAllActiveCustomers() } returns customers

        // When
        val result = customerService.getAllCustomers()

        // Then
        assertNotNull(result)
        assertEquals(2, result.size)
        verify { customerRepository.findAllActiveCustomers() }
    }

    @Test
    fun `updateCustomer should update customer successfully`() {
        // Given
        val updateRequest = UpdateCustomerRequest(
            firstName = "UpdatedJohn",
            lastName = "UpdatedDoe",
            city = "Updated City"
        )
        
        val updatedCustomer = testCustomer.copy(
            firstName = "UpdatedJohn",
            lastName = "UpdatedDoe",
            city = "Updated City"
        )

        every { customerRepository.findByUserId(testUserId) } returns Optional.of(testCustomer)
        every { customerRepository.save(any<Customer>()) } returns updatedCustomer

        // When
        val result = customerService.updateCustomer(testUserId, updateRequest)

        // Then
        assertNotNull(result)
        assertEquals("UpdatedJohn", result.firstName)
        assertEquals("UpdatedDoe", result.lastName)
        assertEquals("Updated City", result.city)
        
        verify { customerRepository.findByUserId(testUserId) }
        verify { customerRepository.save(any<Customer>()) }
        verify { metrics.incrementProfileUpdate() }
    }

    @Test
    fun `updateCustomer should throw exception when customer not found`() {
        // Given
        val updateRequest = UpdateCustomerRequest(firstName = "Updated")
        every { customerRepository.findByUserId(testUserId) } returns Optional.empty()

        // When & Then
        val exception = assertThrows<NoSuchElementException> {
            customerService.updateCustomer(testUserId, updateRequest)
        }
        
        assertEquals("Customer not found with userId: $testUserId", exception.message)
        verify { customerRepository.findByUserId(testUserId) }
        verify(exactly = 0) { customerRepository.save(any<Customer>()) }
    }

    @Test
    fun `deleteCustomer should deactivate customer successfully`() {
        // Given
        every { customerRepository.findByUserId(testUserId) } returns Optional.of(testCustomer)
        every { customerRepository.save(any<Customer>()) } returns testCustomer.copy(isActive = false)

        // When
        customerService.deleteCustomer(testUserId)

        // Then
        verify { customerRepository.findByUserId(testUserId) }
        verify { customerRepository.save(match<Customer> { !it.isActive }) }
    }

    @Test
    fun `loginCustomer should authenticate successfully with username`() {
        // Given
        val loginRequest = LoginRequest(
            identifier = "testuser",
            password = "password123"
        )

        // Clear any previous mocks and set up fresh ones
        clearMocks(customerRepository, passwordUtils, metrics)
        
        // Mock the repository calls
        every { customerRepository.findByUserName("testuser") } returns Optional.of(testCustomer)
        every { customerRepository.findByUserEmail("testuser") } returns Optional.empty()
        every { passwordUtils.verifyPassword("password123", "encrypted_password") } returns true
        every { metrics.incrementUserLogin() } just Runs

        // When
        val result = customerService.loginCustomer(loginRequest)

        // Then
        assertNotNull(result)
        assertEquals(testCustomer.userId, result.userId)
        assertEquals(testCustomer.userName, result.userName)
        assertEquals(testCustomer.userEmail, result.userEmail)
        assertEquals(testCustomer.firstName, result.firstName)
        assertEquals(testCustomer.lastName, result.lastName)
        
        // Verify the calls were made
        verify(exactly = 1) { customerRepository.findByUserName("testuser") }
        verify(exactly = 1) { passwordUtils.verifyPassword("password123", "encrypted_password") }
        verify(exactly = 1) { metrics.incrementUserLogin() }
    }

    @Test
    fun `loginCustomer should authenticate successfully with email`() {
        // Given
        val loginRequest = LoginRequest(
            identifier = "test@example.com",
            password = "password123"
        )

        every { customerRepository.findByUserName(loginRequest.identifier) } returns Optional.empty()
        every { customerRepository.findByUserEmail(loginRequest.identifier) } returns Optional.of(testCustomer)
        every { passwordUtils.verifyPassword(loginRequest.password, testCustomer.password) } returns true

        // When
        val result = customerService.loginCustomer(loginRequest)

        // Then
        assertNotNull(result)
        assertEquals(testCustomer.userId, result.userId)
        assertEquals(testCustomer.userEmail, result.userEmail)
        
        verify { customerRepository.findByUserName(loginRequest.identifier) }
        verify { customerRepository.findByUserEmail(loginRequest.identifier) }
        verify { passwordUtils.verifyPassword(loginRequest.password, testCustomer.password) }
        verify { metrics.incrementUserLogin() }
    }

    @Test
    fun `loginCustomer should throw exception for invalid credentials`() {
        // Given
        val loginRequest = LoginRequest(
            identifier = "testuser",
            password = "wrongpassword"
        )

        // Clear any previous mocks and set up fresh ones
        clearMocks(customerRepository, passwordUtils, metrics)
        
        // Mock the repository calls with explicit values
        // The service calls findByUserName first, then findByUserEmail if first fails
        every { customerRepository.findByUserName("testuser") } returns Optional.of(testCustomer)
        every { customerRepository.findByUserEmail(any()) } returns Optional.empty()
        every { passwordUtils.verifyPassword("wrongpassword", "encrypted_password") } returns false

        // When & Then
        val exception = assertThrows<IllegalArgumentException> {
            customerService.loginCustomer(loginRequest)
        }
        
        assertEquals("Invalid credentials", exception.message)
        verify(exactly = 1) { passwordUtils.verifyPassword("wrongpassword", "encrypted_password") }
    }

    @Test
    fun `loginCustomer should throw exception for inactive customer`() {
        // Given
        val loginRequest = LoginRequest(
            identifier = "testuser",
            password = "password123"
        )
        
        val inactiveCustomer = testCustomer.copy(isActive = false)

        // Clear any previous mocks and set up fresh ones
        clearMocks(customerRepository, passwordUtils, metrics)
        
        // Mock the repository calls with explicit values
        // The service calls findByUserName first, then findByUserEmail if first fails
        every { customerRepository.findByUserName("testuser") } returns Optional.of(inactiveCustomer)
        every { customerRepository.findByUserEmail(any()) } returns Optional.empty()
        every { passwordUtils.verifyPassword("password123", "encrypted_password") } returns true

        // When & Then
        val exception = assertThrows<IllegalArgumentException> {
            customerService.loginCustomer(loginRequest)
        }
        
        assertEquals("Customer account is deactivated", exception.message)
        verify(exactly = 1) { passwordUtils.verifyPassword("password123", "encrypted_password") }
    }

    @Test
    fun `verifyCustomer should verify customer successfully`() {
        // Given
        val verifyRequest = VerifyCustomerRequest(verificationCode = "123456")
        
        // Use reflection to set verification code
        val verificationCodesField = CustomerService::class.java.getDeclaredField("verificationCodes")
        verificationCodesField.isAccessible = true
        val verificationCodes = verificationCodesField.get(customerService) as MutableMap<UUID, String>
        verificationCodes[testUserId] = "123456"

        every { customerRepository.findByUserId(testUserId) } returns Optional.of(testCustomer)
        every { customerRepository.save(any<Customer>()) } returns testCustomer.copy(isVerified = true)

        // When
        val result = customerService.verifyCustomer(testUserId, verifyRequest)

        // Then
        assertNotNull(result)
        assertEquals(testUserId, result.userId)
        assertTrue(result.isVerified)
        assertEquals("Customer verified successfully", result.message)
        
        verify { customerRepository.findByUserId(testUserId) }
        verify { customerRepository.save(match<Customer> { it.isVerified }) }
    }

    @Test
    fun `verifyCustomer should throw exception for invalid code`() {
        // Given
        val verifyRequest = VerifyCustomerRequest(verificationCode = "wrong123")
        
        // Use reflection to set verification code
        val verificationCodesField = CustomerService::class.java.getDeclaredField("verificationCodes")
        verificationCodesField.isAccessible = true
        val verificationCodes = verificationCodesField.get(customerService) as MutableMap<UUID, String>
        verificationCodes[testUserId] = "123456"

        every { customerRepository.findByUserId(testUserId) } returns Optional.of(testCustomer)

        // When & Then
        val exception = assertThrows<IllegalArgumentException> {
            customerService.verifyCustomer(testUserId, verifyRequest)
        }
        
        assertEquals("Invalid verification code", exception.message)
        verify { metrics.incrementApiError() }
    }

    @Test
    fun `resendVerificationCode should resend code successfully`() {
        // Given
        every { customerRepository.findByUserId(testUserId) } returns Optional.of(testCustomer)

        // When
        val result = customerService.resendVerificationCode(testUserId)

        // Then
        assertNotNull(result)
        assertEquals("Verification code sent to ${testCustomer.userEmail}", result)
        verify { customerRepository.findByUserId(testUserId) }
    }

    @Test
    fun `resendVerificationCode should throw exception for already verified customer`() {
        // Given
        val verifiedCustomer = testCustomer.copy(isVerified = true)
        every { customerRepository.findByUserId(testUserId) } returns Optional.of(verifiedCustomer)

        // When & Then
        val exception = assertThrows<IllegalStateException> {
            customerService.resendVerificationCode(testUserId)
        }
        
        assertEquals("Customer is already verified", exception.message)
    }
}
