package com.cred.users.user_service.controller

import com.cred.users.user_service.metrices.Metrics
import com.cred.users.user_service.dto.ApiResponse
import io.mockk.*
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.slf4j.Logger
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import java.util.concurrent.TimeUnit

@ExtendWith(MockKExtension::class)
class BaseControllerTest {

    private lateinit var metrics: Metrics
    private lateinit var logger: Logger
    private lateinit var testController: TestController

    // Test implementation of BaseController
    private class TestController(metrics: Metrics) : BaseController(metrics) {
        
        fun testExecuteWithMetrics(
            logger: Logger,
            operation: String,
            action: () -> String
        ): ResponseEntity<ApiResponse<String>> {
            return executeWithMetrics(logger, operation, action)
        }
        
        fun testExecuteWithCustomResponse(
            logger: Logger,
            operation: String,
            successStatus: HttpStatus,
            successMessage: String,
            action: () -> String
        ): ResponseEntity<ApiResponse<String>> {
            return executeWithCustomResponse(logger, operation, successStatus, successMessage, action)
        }
        
        fun testExecuteAuthOperation(
            logger: Logger,
            operation: String,
            action: () -> String
        ): ResponseEntity<ApiResponse<String>> {
            return executeAuthOperation(logger, operation, action)
        }
    }

    @BeforeEach
    fun setUp() {
        metrics = mockk(relaxed = true)
        logger = mockk(relaxed = true)
        testController = TestController(metrics)
    }

    @Test
    fun `executeWithMetrics should return success response when operation succeeds`() {
        // Given
        val operation = "Test Operation"
        val expectedResult = "Success Result"
        val action: () -> String = { expectedResult }

        // When
        val response = testController.testExecuteWithMetrics(logger, operation, action)

        // Then
        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)
        assertTrue(response.body!!.success)
        assertEquals("$operation completed successfully", response.body!!.message)
        assertEquals(expectedResult, response.body!!.data)
        
        verify { logger.info("Executing operation: $operation") }
        verify { metrics.recordRequestLatency(any(), TimeUnit.MILLISECONDS) }
    }

    @Test
    fun `executeWithMetrics should return 404 when NoSuchElementException is thrown`() {
        // Given
        val operation = "Test Operation"
        val errorMessage = "Resource not found"
        val action: () -> String = { throw NoSuchElementException(errorMessage) }

        // When
        val response = testController.testExecuteWithMetrics(logger, operation, action)

        // Then
        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
        assertNotNull(response.body)
        assertFalse(response.body!!.success)
        assertEquals(errorMessage, response.body!!.message)
        assertNull(response.body!!.data)
        
        verify { logger.error("Resource not found during $operation: $errorMessage") }
    }

    @Test
    fun `executeWithMetrics should return 400 when IllegalArgumentException is thrown`() {
        // Given
        val operation = "Test Operation"
        val errorMessage = "Invalid input"
        val action: () -> String = { throw IllegalArgumentException(errorMessage) }

        // When
        val response = testController.testExecuteWithMetrics(logger, operation, action)

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertNotNull(response.body)
        assertFalse(response.body!!.success)
        assertEquals(errorMessage, response.body!!.message)
        assertNull(response.body!!.data)
        
        verify { logger.error("Invalid input during $operation: $errorMessage") }
        verify { metrics.incrementApiError() }
    }

    @Test
    fun `executeWithMetrics should return 400 when IllegalStateException is thrown`() {
        // Given
        val operation = "Test Operation"
        val errorMessage = "Invalid state"
        val action: () -> String = { throw IllegalStateException(errorMessage) }

        // When
        val response = testController.testExecuteWithMetrics(logger, operation, action)

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertNotNull(response.body)
        assertFalse(response.body!!.success)
        assertEquals(errorMessage, response.body!!.message)
        assertNull(response.body!!.data)
        
        verify { logger.error("Invalid state during $operation: $errorMessage") }
        verify { metrics.incrementApiError() }
    }

    @Test
    fun `executeWithMetrics should return 500 when unexpected exception is thrown`() {
        // Given
        val operation = "Test Operation"
        val action: () -> String = { throw RuntimeException("Unexpected error") }

        // When
        val response = testController.testExecuteWithMetrics(logger, operation, action)

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
        assertNotNull(response.body)
        assertFalse(response.body!!.success)
        assertEquals("Internal server error", response.body!!.message)
        assertNull(response.body!!.data)
        
        verify { logger.error("Unexpected error during $operation", any<RuntimeException>()) }
        verify { metrics.incrementApiError() }
    }

    @Test
    fun `executeWithCustomResponse should return custom status and message when operation succeeds`() {
        // Given
        val operation = "Custom Operation"
        val expectedResult = "Custom Result"
        val customStatus = HttpStatus.CREATED
        val customMessage = "Custom success message"
        val action: () -> String = { expectedResult }

        // When
        val response = testController.testExecuteWithCustomResponse(
            logger, operation, customStatus, customMessage, action
        )

        // Then
        assertEquals(customStatus, response.statusCode)
        assertNotNull(response.body)
        assertTrue(response.body!!.success)
        assertEquals(customMessage, response.body!!.message)
        assertEquals(expectedResult, response.body!!.data)
        
        verify { logger.info("Executing operation: $operation") }
        verify { metrics.recordRequestLatency(any(), TimeUnit.MILLISECONDS) }
    }

    @Test
    fun `executeWithCustomResponse should handle exceptions same as executeWithMetrics`() {
        // Given
        val operation = "Custom Operation"
        val customStatus = HttpStatus.CREATED
        val customMessage = "Custom success message"
        val errorMessage = "Resource not found"
        val action: () -> String = { throw NoSuchElementException(errorMessage) }

        // When
        val response = testController.testExecuteWithCustomResponse(
            logger, operation, customStatus, customMessage, action
        )

        // Then
        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
        assertNotNull(response.body)
        assertFalse(response.body!!.success)
        assertEquals(errorMessage, response.body!!.message)
        assertNull(response.body!!.data)
        
        verify { logger.error("Resource not found during $operation: $errorMessage") }
    }

    @Test
    fun `executeAuthOperation should return success response when authentication succeeds`() {
        // Given
        val operation = "Login"
        val expectedResult = "Auth Success"
        val action: () -> String = { expectedResult }

        // When
        val response = testController.testExecuteAuthOperation(logger, operation, action)

        // Then
        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)
        assertTrue(response.body!!.success)
        assertEquals("$operation successful", response.body!!.message)
        assertEquals(expectedResult, response.body!!.data)
        
        verify { logger.info("Executing auth operation: $operation") }
        verify { metrics.recordRequestLatency(any(), TimeUnit.MILLISECONDS) }
    }

    @Test
    fun `executeAuthOperation should return 401 when NoSuchElementException is thrown`() {
        // Given
        val operation = "Login"
        val action: () -> String = { throw NoSuchElementException("User not found") }

        // When
        val response = testController.testExecuteAuthOperation(logger, operation, action)

        // Then
        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
        assertNotNull(response.body)
        assertFalse(response.body!!.success)
        assertEquals("Invalid credentials", response.body!!.message)
        assertNull(response.body!!.data)
        
        verify { logger.error("Authentication failed during $operation") }
    }

    @Test
    fun `executeAuthOperation should return 401 when IllegalArgumentException is thrown`() {
        // Given
        val operation = "Login"
        val action: () -> String = { throw IllegalArgumentException("Invalid password") }

        // When
        val response = testController.testExecuteAuthOperation(logger, operation, action)

        // Then
        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
        assertNotNull(response.body)
        assertFalse(response.body!!.success)
        assertEquals("Invalid credentials", response.body!!.message)
        assertNull(response.body!!.data)
        
        verify { logger.error("Authentication failed during $operation") }
        verify { metrics.incrementApiError() }
    }

    @Test
    fun `executeAuthOperation should return 500 when unexpected exception is thrown`() {
        // Given
        val operation = "Login"
        val action: () -> String = { throw RuntimeException("Database connection failed") }

        // When
        val response = testController.testExecuteAuthOperation(logger, operation, action)

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
        assertNotNull(response.body)
        assertFalse(response.body!!.success)
        assertEquals("Internal server error", response.body!!.message)
        assertNull(response.body!!.data)
        
        verify { logger.error("Unexpected error during $operation", any<RuntimeException>()) }
        verify { metrics.incrementApiError() }
    }

    @Test
    fun `all execute methods should handle null exception messages gracefully`() {
        // Given
        val operation = "Test Operation"
        val actionWithNullMessage: () -> String = { 
            throw IllegalArgumentException(null as String?) 
        }

        // When
        val response1 = testController.testExecuteWithMetrics(logger, operation, actionWithNullMessage)
        val response2 = testController.testExecuteWithCustomResponse(
            logger, operation, HttpStatus.OK, "Success", actionWithNullMessage
        )

        // Then
        assertEquals("Invalid input data", response1.body!!.message)
        assertEquals("Invalid input data", response2.body!!.message)
    }

    @Test
    fun `metrics should be recorded for all successful operations`() {
        // Given
        val operation = "Test Operation"
        val action: () -> String = { "Success" }

        // When
        testController.testExecuteWithMetrics(logger, operation, action)
        testController.testExecuteWithCustomResponse(logger, operation, HttpStatus.OK, "Success", action)
        testController.testExecuteAuthOperation(logger, operation, action)

        // Then
        verify(exactly = 3) { metrics.recordRequestLatency(any(), TimeUnit.MILLISECONDS) }
    }

    @Test
    fun `error metrics should be incremented for appropriate exceptions`() {
        // Given
        val operation = "Test Operation"
        val illegalArgAction: () -> String = { throw IllegalArgumentException("Error") }
        val illegalStateAction: () -> String = { throw IllegalStateException("Error") }
        val runtimeAction: () -> String = { throw RuntimeException("Error") }

        // When
        testController.testExecuteWithMetrics(logger, operation, illegalArgAction)
        testController.testExecuteWithMetrics(logger, operation, illegalStateAction)
        testController.testExecuteWithMetrics(logger, operation, runtimeAction)
        testController.testExecuteAuthOperation(logger, operation, illegalArgAction)
        testController.testExecuteAuthOperation(logger, operation, runtimeAction)

        // Then
        verify(exactly = 5) { metrics.incrementApiError() }
    }
}
