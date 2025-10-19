package com.cred.users.user_service.repositories

import com.cred.users.user_service.repositories.entity.Customer
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface CustomerRepository : JpaRepository<Customer, Long> {

    // Find by UUID (user_id) - this is what we'll expose to users
    fun findByUserId(userId: UUID): Optional<Customer>

    // Find by username
    fun findByUserName(userName: String): Optional<Customer>

    // Find by user email
    fun findByUserEmail(userEmail: String): Optional<Customer>

    // Find by phone number
    fun findByPhoneNumber(phoneNumber: String): Optional<Customer>

    // Check existence by UUID
    fun existsByUserId(userId: UUID): Boolean

    // Check existence by username
    fun existsByUserName(userName: String): Boolean

    // Check existence by user email
    fun existsByUserEmail(userEmail: String): Boolean

    // Check existence by phone number
    fun existsByPhoneNumber(phoneNumber: String): Boolean

    @Query("SELECT c FROM Customer c WHERE c.isActive = true")
    fun findAllActiveCustomers(): List<Customer>

    @Query("SELECT c FROM Customer c WHERE c.isVerified = true AND c.isActive = true")
    fun findAllVerifiedCustomers(): List<Customer>
}