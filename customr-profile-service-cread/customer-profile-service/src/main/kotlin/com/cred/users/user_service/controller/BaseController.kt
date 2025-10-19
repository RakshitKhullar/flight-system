package com.cred.users.user_service.controller

import com.cred.users.user_service.dto.ApiResponse
import com.cred.users.user_service.metrices.Metrics
import org.slf4j.Logger
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import java.util.concurrent.TimeUnit

/**
 * Base Controller following DRY principle
 * Contains common functionality to avoid code duplication
 */
abstract class BaseController(
    protected val metrics: Metrics
) {
    
    /**
     * Execute operation with common error handling and metrics
     * Follows DRY principle by centralizing common controller logic
     */
    protected inline fun <T> executeWithMetrics(
        logger: Logger,
        operation: String,
        crossinline action: () -> T
    ): ResponseEntity<ApiResponse<T>> {
        val startTime = System.currentTimeMillis()
        
        return try {
            logger.info("Executing operation: $operation")
            
            val result = action()
            val response = ApiResponse(
                success = true,
                message = "$operation completed successfully",
                data = result
            )
            
            recordLatency(startTime)
            ResponseEntity.ok(response)
            
        } catch (e: NoSuchElementException) {
            logger.error("Resource not found during $operation: ${e.message}")
            
            val response = ApiResponse<T>(
                success = false,
                message = e.message ?: "Resource not found"
            )
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(response)
            
        } catch (e: IllegalArgumentException) {
            logger.error("Invalid input during $operation: ${e.message}")
            metrics.incrementApiError()
            
            val response = ApiResponse<T>(
                success = false,
                message = e.message ?: "Invalid input data"
            )
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response)
            
        } catch (e: IllegalStateException) {
            logger.error("Invalid state during $operation: ${e.message}")
            metrics.incrementApiError()
            
            val response = ApiResponse<T>(
                success = false,
                message = e.message ?: "Invalid operation state"
            )
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response)
            
        } catch (e: Exception) {
            logger.error("Unexpected error during $operation", e)
            metrics.incrementApiError()
            
            val response = ApiResponse<T>(
                success = false,
                message = "Internal server error"
            )
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response)
        }
    }
    
    /**
     * Execute operation with custom success status and message
     */
    protected inline fun <T> executeWithCustomResponse(
        logger: Logger,
        operation: String,
        successStatus: HttpStatus,
        successMessage: String,
        crossinline action: () -> T
    ): ResponseEntity<ApiResponse<T>> {
        val startTime = System.currentTimeMillis()
        
        return try {
            logger.info("Executing operation: $operation")
            
            val result = action()
            val response = ApiResponse(
                success = true,
                message = successMessage,
                data = result
            )
            
            recordLatency(startTime)
            ResponseEntity.status(successStatus).body(response)
            
        } catch (e: NoSuchElementException) {
            logger.error("Resource not found during $operation: ${e.message}")
            
            val response = ApiResponse<T>(
                success = false,
                message = e.message ?: "Resource not found"
            )
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(response)
            
        } catch (e: IllegalArgumentException) {
            logger.error("Invalid input during $operation: ${e.message}")
            metrics.incrementApiError()
            
            val response = ApiResponse<T>(
                success = false,
                message = e.message ?: "Invalid input data"
            )
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response)
            
        } catch (e: IllegalStateException) {
            logger.error("Invalid state during $operation: ${e.message}")
            metrics.incrementApiError()
            
            val response = ApiResponse<T>(
                success = false,
                message = e.message ?: "Invalid operation state"
            )
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response)
            
        } catch (e: Exception) {
            logger.error("Unexpected error during $operation", e)
            metrics.incrementApiError()
            
            val response = ApiResponse<T>(
                success = false,
                message = "Internal server error"
            )
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response)
        }
    }
    
    /**
     * Execute operation for authentication with specific error handling
     */
    protected inline fun <T> executeAuthOperation(
        logger: Logger,
        operation: String,
        crossinline action: () -> T
    ): ResponseEntity<ApiResponse<T>> {
        val startTime = System.currentTimeMillis()
        
        return try {
            logger.info("Executing auth operation: $operation")
            
            val result = action()
            val response = ApiResponse(
                success = true,
                message = "$operation successful",
                data = result
            )
            
            recordLatency(startTime)
            ResponseEntity.ok(response)
            
        } catch (e: NoSuchElementException) {
            logger.error("Authentication failed during $operation")
            
            val response = ApiResponse<T>(
                success = false,
                message = "Invalid credentials"
            )
            ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response)
            
        } catch (e: IllegalArgumentException) {
            logger.error("Authentication failed during $operation")
            metrics.incrementApiError()
            
            val response = ApiResponse<T>(
                success = false,
                message = "Invalid credentials"
            )
            ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response)
            
        } catch (e: Exception) {
            logger.error("Unexpected error during $operation", e)
            metrics.incrementApiError()
            
            val response = ApiResponse<T>(
                success = false,
                message = "Internal server error"
            )
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response)
        }
    }
    
    protected fun recordLatency(startTime: Long) {
        metrics.recordRequestLatency(System.currentTimeMillis() - startTime, TimeUnit.MILLISECONDS)
    }
}
