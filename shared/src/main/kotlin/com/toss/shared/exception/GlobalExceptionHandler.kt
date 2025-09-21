package com.toss.shared.exception

import com.toss.shared.dto.ApiResponse
import com.toss.shared.dto.ErrorResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.AuthenticationException
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest
import java.time.LocalDateTime

@RestControllerAdvice
class GlobalExceptionHandler {

    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(TossException::class)
    fun handleTossException(ex: TossException, request: WebRequest): ResponseEntity<ApiResponse<Nothing>> {
        logger.warn("TossException occurred: ${ex.message}", ex)
        return ResponseEntity.status(ex.status)
            .body(ApiResponse.error(ex.errorCode, ex.message ?: "Unknown error"))
    }

    @ExceptionHandler(AuthenticationException::class)
    fun handleAuthenticationException(ex: AuthenticationException, request: WebRequest): ResponseEntity<ApiResponse<Nothing>> {
        logger.warn("AuthenticationException occurred: ${ex.message}", ex)
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(ApiResponse.error("AUTH_001", "Authentication failed"))
    }

    @ExceptionHandler(BadCredentialsException::class)
    fun handleBadCredentialsException(ex: BadCredentialsException, request: WebRequest): ResponseEntity<ApiResponse<Nothing>> {
        logger.warn("BadCredentialsException occurred: ${ex.message}", ex)
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(ApiResponse.error("AUTH_002", "Invalid credentials"))
    }

    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDeniedException(ex: AccessDeniedException, request: WebRequest): ResponseEntity<ApiResponse<Nothing>> {
        logger.warn("AccessDeniedException occurred: ${ex.message}", ex)
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(ApiResponse.error("AUTH_003", "Access denied"))
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(ex: MethodArgumentNotValidException, request: WebRequest): ResponseEntity<ApiResponse<Nothing>> {
        logger.warn("ValidationException occurred: ${ex.message}", ex)
        val errors = ex.bindingResult.fieldErrors.associate { 
            it.field to (it.defaultMessage ?: "Invalid value")
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error("VALID_001", "Validation failed", errors))
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(ex: IllegalArgumentException, request: WebRequest): ResponseEntity<ApiResponse<Nothing>> {
        logger.warn("IllegalArgumentException occurred: ${ex.message}", ex)
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error("INVALID_001", ex.message ?: "Invalid argument"))
    }

    @ExceptionHandler(Exception::class)
    fun handleGenericException(ex: Exception, request: WebRequest): ResponseEntity<ApiResponse<Nothing>> {
        logger.error("Unexpected exception occurred: ${ex.message}", ex)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error("INTERNAL_001", "Internal server error"))
    }
}
