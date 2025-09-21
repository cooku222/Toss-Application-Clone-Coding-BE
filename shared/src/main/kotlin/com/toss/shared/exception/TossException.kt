package com.toss.shared.exception

import org.springframework.http.HttpStatus

class TossException(
    val errorCode: String,
    message: String? = null,
    val status: HttpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
    cause: Throwable? = null
) : RuntimeException(message, cause)

// Common error codes
object ErrorCodes {
    // Authentication & Authorization
    const val AUTH_TOKEN_EXPIRED = "AUTH_001"
    const val AUTH_TOKEN_INVALID = "AUTH_002"
    const val AUTH_ACCESS_DENIED = "AUTH_003"
    const val AUTH_USER_NOT_FOUND = "AUTH_004"
    const val AUTH_PASSWORD_INCORRECT = "AUTH_005"
    
    // Account
    const val ACCOUNT_NOT_FOUND = "ACC_001"
    const val ACCOUNT_INSUFFICIENT_BALANCE = "ACC_002"
    const val ACCOUNT_ALREADY_EXISTS = "ACC_003"
    const val ACCOUNT_INACTIVE = "ACC_004"
    
    // Transaction
    const val TRANSACTION_NOT_FOUND = "TXN_001"
    const val TRANSACTION_INVALID_AMOUNT = "TXN_002"
    const val TRANSACTION_DUPLICATE = "TXN_003"
    const val TRANSACTION_LIMIT_EXCEEDED = "TXN_004"
    const val TRANSACTION_INVALID_RECIPIENT = "TXN_005"
    
    // Ledger
    const val LEDGER_ENTRY_FAILED = "LED_001"
    const val LEDGER_BALANCE_MISMATCH = "LED_002"
    
    // Notification
    const val NOTIFICATION_SEND_FAILED = "NOT_001"
    const val NOTIFICATION_TEMPLATE_NOT_FOUND = "NOT_002"
    
    // Validation
    const val VALIDATION_FAILED = "VAL_001"
    const val VALIDATION_INVALID_FORMAT = "VAL_002"
    
    // External Service
    const val EXTERNAL_SERVICE_UNAVAILABLE = "EXT_001"
    const val EXTERNAL_SERVICE_TIMEOUT = "EXT_002"
    
    // Internal
    const val INTERNAL_ERROR = "INT_001"
    const val INTERNAL_DATABASE_ERROR = "INT_002"
    const val INTERNAL_CACHE_ERROR = "INT_003"
}
