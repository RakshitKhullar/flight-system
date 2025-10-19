package com.cred.users.user_service.controller

import com.cred.users.user_service.dto.ApiResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {
    
    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)
    
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(ex: MethodArgumentNotValidException): ResponseEntity<ApiResponse<Nothing>> {
        logger.error("Validation failed: ${ex.message}")
        
        val errors = ex.bindingResult.fieldErrors.map { 
            "${it.field}: ${it.defaultMessage}" 
        }
        
        val response = ApiResponse<Nothing>(
            success = false,
            message = "Validation failed: ${errors.joinToString(", ")}"
        )
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response)
    }
    
    @ExceptionHandler(jakarta.validation.ConstraintViolationException::class)
    fun handleConstraintViolationException(ex: jakarta.validation.ConstraintViolationException): ResponseEntity<ApiResponse<Nothing>> {
        logger.error("Constraint validation failed: ${ex.message}")
        
        val errors = ex.constraintViolations.map { 
            "${it.propertyPath}: ${it.message}" 
        }
        
        val response = ApiResponse<Nothing>(
            success = false,
            message = "Validation failed: ${errors.joinToString(", ")}"
        )
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response)
    }
}
