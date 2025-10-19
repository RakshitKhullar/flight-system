package com.cred.users.user_service.service

import com.cred.users.user_service.dto.*
import com.cred.users.user_service.repositories.entity.Customer
import com.cred.users.user_service.repositories.CustomerRepository
import com.cred.users.user_service.metrices.Metrics
import com.cred.users.user_service.utils.PasswordUtils
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.*
import kotlin.random.Random

@Service
@Transactional
class CustomerService(
    private val customerRepository: CustomerRepository,
    private val metrics: Metrics,
    private val passwordUtils: PasswordUtils
) : CustomerServiceInterface {
    private val logger = LoggerFactory.getLogger(CustomerService::class.java)
    
    // In-memory store for verification codes (in production, use Redis or database)
    private val verificationCodes = mutableMapOf<UUID, String>()

    override fun createCustomer(request: CreateCustomerRequest): CustomerResponse {
        logger.info("Creating customer with email: ${request.userEmail}")
        
        // Check if customer already exists
        if (customerRepository.existsByUserEmail(request.userEmail)) {
            throw IllegalArgumentException("Customer with email ${request.userEmail} already exists")
        }
        
        if (customerRepository.existsByUserName(request.userName)) {
            throw IllegalArgumentException("Customer with username ${request.userName} already exists")
        }
        
        if (customerRepository.existsByPhoneNumber(request.phoneNumber)) {
            throw IllegalArgumentException("Customer with phone number ${request.phoneNumber} already exists")
        }

        // Encrypt password
        val encryptedPassword = passwordUtils.encryptPassword(request.password)

        val customer = Customer(
            userName = request.userName,
            userDob = request.userDob,
            userEmail = request.userEmail,
            password = encryptedPassword,
            firstName = request.firstName,
            lastName = request.lastName,
            phoneNumber = request.phoneNumber,
            address = request.address,
            city = request.city,
            country = request.country,
            postalCode = request.postalCode
        )

        val savedCustomer = customerRepository.save(customer)
        
        // Generate verification code using UUID
        generateVerificationCode(savedCustomer.userId)
        
        metrics.incrementUserRegistration()
        logger.info("Customer created successfully with userId: ${savedCustomer.userId}")
        
        return mapToCustomerResponse(savedCustomer)
    }

    override fun getCustomerByUserId(userId: UUID): CustomerResponse {
        logger.info("Fetching customer with userId: $userId")
        
        val customer = customerRepository.findByUserId(userId)
            .orElseThrow { NoSuchElementException("Customer not found with userId: $userId") }
            
        return mapToCustomerResponse(customer)
    }

    override fun getAllCustomers(): List<CustomerResponse> {
        logger.info("Fetching all active customers")
        
        return customerRepository.findAllActiveCustomers()
            .map { mapToCustomerResponse(it) }
    }

    override fun updateCustomer(userId: UUID, request: UpdateCustomerRequest): CustomerResponse {
        logger.info("Updating customer with userId: $userId")
        
        val existingCustomer = customerRepository.findByUserId(userId)
            .orElseThrow { NoSuchElementException("Customer not found with userId: $userId") }

        // Check for email uniqueness if email is being updated
        request.userEmail?.let { newEmail ->
            if (newEmail != existingCustomer.userEmail && customerRepository.existsByUserEmail(newEmail)) {
                throw IllegalArgumentException("Customer with email $newEmail already exists")
            }
        }

        // Check for username uniqueness if username is being updated
        request.userName?.let { newUserName ->
            if (newUserName != existingCustomer.userName && customerRepository.existsByUserName(newUserName)) {
                throw IllegalArgumentException("Customer with username $newUserName already exists")
            }
        }

        // Check for phone number uniqueness if phone is being updated
        request.phoneNumber?.let { newPhone ->
            if (newPhone != existingCustomer.phoneNumber && customerRepository.existsByPhoneNumber(newPhone)) {
                throw IllegalArgumentException("Customer with phone number $newPhone already exists")
            }
        }

        val updatedCustomer = existingCustomer.copy(
            userName = request.userName ?: existingCustomer.userName,
            userDob = request.userDob ?: existingCustomer.userDob,
            userEmail = request.userEmail ?: existingCustomer.userEmail,
            firstName = request.firstName ?: existingCustomer.firstName,
            lastName = request.lastName ?: existingCustomer.lastName,
            phoneNumber = request.phoneNumber ?: existingCustomer.phoneNumber,
            address = request.address ?: existingCustomer.address,
            city = request.city ?: existingCustomer.city,
            country = request.country ?: existingCustomer.country,
            postalCode = request.postalCode ?: existingCustomer.postalCode,
            updatedAt = LocalDateTime.now()
        )

        val savedCustomer = customerRepository.save(updatedCustomer)
        metrics.incrementProfileUpdate()
        logger.info("Customer updated successfully with userId: $userId")
        
        return mapToCustomerResponse(savedCustomer)
    }

    override fun deleteCustomer(userId: UUID) {
        logger.info("Deactivating customer with userId: $userId")
        
        val customer = customerRepository.findByUserId(userId)
            .orElseThrow { NoSuchElementException("Customer not found with userId: $userId") }

        val deactivatedCustomer = customer.copy(
            isActive = false,
            updatedAt = LocalDateTime.now()
        )

        customerRepository.save(deactivatedCustomer)
        logger.info("Customer deactivated successfully with userId: $userId")
    }

    override fun verifyCustomer(userId: UUID, request: VerifyCustomerRequest): CustomerVerificationResponse {
        logger.info("Verifying customer with userId: $userId")
        
        val customer = customerRepository.findByUserId(userId)
            .orElseThrow { NoSuchElementException("Customer not found with userId: $userId") }

        val storedCode = verificationCodes[userId]
            ?: throw IllegalStateException("No verification code found for customer userId: $userId")

        if (storedCode != request.verificationCode) {
            metrics.incrementApiError()
            throw IllegalArgumentException("Invalid verification code")
        }

        val verifiedCustomer = customer.copy(
            isVerified = true,
            updatedAt = LocalDateTime.now()
        )

        customerRepository.save(verifiedCustomer)
        verificationCodes.remove(userId) // Remove used verification code
        
        logger.info("Customer verified successfully with userId: $userId")
        
        return CustomerVerificationResponse(
            userId = userId,
            isVerified = true,
            message = "Customer verified successfully"
        )
    }

    override fun resendVerificationCode(userId: UUID): String {
        logger.info("Resending verification code for customer userId: $userId")
        
        val customer = customerRepository.findByUserId(userId)
            .orElseThrow { NoSuchElementException("Customer not found with userId: $userId") }

        if (customer.isVerified) {
            throw IllegalStateException("Customer is already verified")
        }

        val code = generateVerificationCode(userId)
        logger.info("Verification code resent for customer userId: $userId")
        
        return "Verification code sent to ${customer.userEmail}"
    }

    override fun loginCustomer(request: LoginRequest): CustomerResponse {
        logger.info("Login attempt for identifier: ${request.identifier}")
        
        // Try to find customer by username or email
        val customer = customerRepository.findByUserName(request.identifier)
            .orElseGet { 
                customerRepository.findByUserEmail(request.identifier)
                    .orElseThrow { NoSuchElementException("Customer not found") }
            }
        
        // Verify password
        if (!passwordUtils.verifyPassword(request.password, customer.password)) {
            throw IllegalArgumentException("Invalid credentials")
        }
        
        // Check if customer is active
        if (!customer.isActive) {
            throw IllegalArgumentException("Customer account is deactivated")
        }
        
        metrics.incrementUserLogin()
        logger.info("Login successful for userId: ${customer.userId}")
        
        return mapToCustomerResponse(customer)
    }

    private fun generateVerificationCode(userId: UUID): String {
        val code = Random.nextInt(100000, 999999).toString()
        verificationCodes[userId] = code
        
        // In production, send this code via email/SMS
        logger.info("Generated verification code for customer userId $userId: $code")
        
        return code
    }

    private fun mapToCustomerResponse(customer: Customer): CustomerResponse {
        return CustomerResponse(
            userId = customer.userId,
            userName = customer.userName,
            userDob = customer.userDob,
            userEmail = customer.userEmail,
            firstName = customer.firstName,
            lastName = customer.lastName,
            phoneNumber = customer.phoneNumber,
            address = customer.address,
            city = customer.city,
            country = customer.country,
            postalCode = customer.postalCode,
            isVerified = customer.isVerified,
            isActive = customer.isActive,
            createdAt = customer.createdAt,
            updatedAt = customer.updatedAt
        )
    }
}
