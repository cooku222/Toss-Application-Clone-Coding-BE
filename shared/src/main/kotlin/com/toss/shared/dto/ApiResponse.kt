package com.toss.shared.dto

import com.fasterxml.jackson.annotation.JsonInclude
import java.time.LocalDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val error: ErrorResponse? = null,
    val timestamp: LocalDateTime = LocalDateTime.now()
) {
    companion object {
        fun <T> success(data: T): ApiResponse<T> {
            return ApiResponse(success = true, data = data)
        }
        
        fun <T> success(): ApiResponse<T> {
            return ApiResponse(success = true, data = null)
        }
        
        fun <T> error(errorCode: String, message: String, details: Any? = null): ApiResponse<T> {
            return ApiResponse(
                success = false,
                error = ErrorResponse(errorCode, message, details)
            )
        }
    }
}

data class ErrorResponse(
    val code: String,
    val message: String,
    val details: Any? = null
)

// Common DTOs
data class PageResponse<T>(
    val content: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val first: Boolean,
    val last: Boolean
)

data class IdempotencyRequest(
    val idempotencyKey: String,
    val requestId: String
)

data class AuditInfo(
    val createdBy: String,
    val createdAt: LocalDateTime,
    val updatedBy: String? = null,
    val updatedAt: LocalDateTime? = null
)
