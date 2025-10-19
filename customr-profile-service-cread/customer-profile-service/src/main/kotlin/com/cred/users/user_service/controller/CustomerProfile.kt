package com.cred.users.user_service.controller

import com.cred.users.user_service.dto.*
import com.cred.users.user_service.service.CustomerServiceInterface
import com.cred.users.user_service.metrices.Metrics
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.concurrent.TimeUnit
import java.util.*

@RestController
@RequestMapping("/api/v1/customer")
@Tag(name = "Customer Profile", description = "APIs for managing customer profiles")
class CustomerProfile(
    private val customerService: CustomerServiceInterface,
    metrics: Metrics
) : BaseController(metrics) {
    private val logger = LoggerFactory.getLogger(CustomerProfile::class.java)

    @PostMapping
    @Operation(summary = "Create a new customer", description = "Creates a new customer profile")
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "201", description = "Customer created successfully"),
            SwaggerApiResponse(responseCode = "400", description = "Invalid input data"),
            SwaggerApiResponse(responseCode = "409", description = "Customer already exists")
        ]
    )
    fun createCustomer(
        @Valid @RequestBody request: CreateCustomerRequest
    ): ResponseEntity<ApiResponse<CustomerResponse>> {
        return executeWithCustomResponse(
            logger = logger,
            operation = "Create customer with email: ${request.userEmail}",
            successStatus = HttpStatus.CREATED,
            successMessage = "Customer created successfully. Verification code sent to email."
        ) {
            customerService.createCustomer(request)
        }
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Get customer by User ID", description = "Retrieves a customer profile by User ID")
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "Customer found"),
            SwaggerApiResponse(responseCode = "404", description = "Customer not found")
        ]
    )
    fun getCustomerByUserId(
        @Parameter(description = "Customer User ID") @PathVariable userId: UUID
    ): ResponseEntity<ApiResponse<CustomerResponse>> {
        return executeWithMetrics(
            logger = logger,
            operation = "Get customer by userId: $userId"
        ) {
            customerService.getCustomerByUserId(userId)
        }
    }

    @GetMapping
    @Operation(summary = "Get all customers", description = "Retrieves all active customer profiles")
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "Customers retrieved successfully")
        ]
    )
    fun getAllCustomers(): ResponseEntity<ApiResponse<List<CustomerResponse>>> {
        return executeWithMetrics(
            logger = logger,
            operation = "Get all customers"
        ) {
            customerService.getAllCustomers()
        }
    }

    @PutMapping("/{userId}")
    @Operation(summary = "Update customer", description = "Updates an existing customer profile")
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "Customer updated successfully"),
            SwaggerApiResponse(responseCode = "400", description = "Invalid input data"),
            SwaggerApiResponse(responseCode = "404", description = "Customer not found")
        ]
    )
    fun updateCustomer(
        @Parameter(description = "Customer User ID") @PathVariable userId: UUID,
        @Valid @RequestBody request: UpdateCustomerRequest
    ): ResponseEntity<ApiResponse<CustomerResponse>> {
        return executeWithMetrics(
            logger = logger,
            operation = "Update customer with userId: $userId"
        ) {
            customerService.updateCustomer(userId, request)
        }
    }

    @DeleteMapping("/{userId}")
    @Operation(summary = "Delete customer", description = "Deactivates a customer profile")
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "Customer deactivated successfully"),
            SwaggerApiResponse(responseCode = "404", description = "Customer not found")
        ]
    )
    fun deleteCustomer(
        @Parameter(description = "Customer User ID") @PathVariable userId: UUID
    ): ResponseEntity<ApiResponse<String>> {
        return executeWithCustomResponse(
            logger = logger,
            operation = "Delete customer with userId: $userId",
            successStatus = HttpStatus.OK,
            successMessage = "Customer deactivated successfully"
        ) {
            customerService.deleteCustomer(userId)
            "Customer with userId $userId has been deactivated"
        }
    }

    @PostMapping("/{userId}/verify")
    @Operation(summary = "Verify customer", description = "Verifies a customer using verification code")
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "Customer verified successfully"),
            SwaggerApiResponse(responseCode = "400", description = "Invalid verification code"),
            SwaggerApiResponse(responseCode = "404", description = "Customer not found")
        ]
    )
    fun verifyCustomer(
        @Parameter(description = "Customer User ID") @PathVariable userId: UUID,
        @Valid @RequestBody request: VerifyCustomerRequest
    ): ResponseEntity<ApiResponse<CustomerVerificationResponse>> {
        return executeWithMetrics(
            logger = logger,
            operation = "Verify customer with userId: $userId"
        ) {
            customerService.verifyCustomer(userId, request)
        }
    }

    @PostMapping("/{userId}/resend-verification")
    @Operation(summary = "Resend verification code", description = "Resends verification code to customer")
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "Verification code sent successfully"),
            SwaggerApiResponse(responseCode = "400", description = "Customer already verified"),
            SwaggerApiResponse(responseCode = "404", description = "Customer not found")
        ]
    )
    fun resendVerificationCode(
        @Parameter(description = "Customer User ID") @PathVariable userId: UUID
    ): ResponseEntity<ApiResponse<String>> {
        return executeWithCustomResponse(
            logger = logger,
            operation = "Resend verification code for userId: $userId",
            successStatus = HttpStatus.OK,
            successMessage = "Verification code resent successfully"
        ) {
            customerService.resendVerificationCode(userId)
            "Verification code resent successfully"
        }
    }

    @PostMapping("/login")
    @Operation(summary = "Customer login", description = "Authenticates a customer using username/email and password")
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "Login successful"),
            SwaggerApiResponse(responseCode = "401", description = "Invalid credentials"),
            SwaggerApiResponse(responseCode = "404", description = "Customer not found")
        ]
    )
    fun loginCustomer(
        @Valid @RequestBody request: LoginRequest
    ): ResponseEntity<ApiResponse<CustomerResponse>> {
        return executeAuthOperation(
            logger = logger,
            operation = "Customer login for: ${request.identifier}"
        ) {
            customerService.loginCustomer(request).also {
                metrics.incrementUserLogin()
            }
        }
    }
}