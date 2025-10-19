package com.cred.users.user_service.service

import com.cred.users.user_service.dto.*
import java.util.*

/**
 * Customer Service Interface following SOLID principles
 * - Single Responsibility: Only customer-related operations
 * - Interface Segregation: Focused interface for customer operations
 */
interface CustomerServiceInterface {
    
    /**
     * Customer Management Operations
     */
    fun createCustomer(request: CreateCustomerRequest): CustomerResponse
    fun getCustomerByUserId(userId: UUID): CustomerResponse
    fun getAllCustomers(): List<CustomerResponse>
    fun updateCustomer(userId: UUID, request: UpdateCustomerRequest): CustomerResponse
    fun deleteCustomer(userId: UUID)
    
    /**
     * Authentication Operations
     */
    fun loginCustomer(request: LoginRequest): CustomerResponse
    
    /**
     * Verification Operations
     */
    fun verifyCustomer(userId: UUID, request: VerifyCustomerRequest): CustomerVerificationResponse
    fun resendVerificationCode(userId: UUID): String
}
