package com.toss.transaction.dto

import com.toss.transaction.entity.TransactionType
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.math.BigDecimal

data class CreateTransactionRequest(
    @field:NotBlank(message = "From account number is required")
    val fromAccountNumber: String,
    
    @field:NotBlank(message = "To account number is required")
    val toAccountNumber: String,
    
    @field:NotNull(message = "Amount is required")
    @field:DecimalMin(value = "1.0", message = "Amount must be at least 1")
    @field:DecimalMax(value = "10000000.0", message = "Amount cannot exceed 10M")
    val amount: BigDecimal,
    
    @field:Size(max = 100, message = "Description cannot exceed 100 characters")
    val description: String? = null,
    
    @field:NotNull(message = "Transaction type is required")
    val transactionType: TransactionType,
    
    @field:NotBlank(message = "Idempotency key is required")
    val idempotencyKey: String
)

data class UpdateTransactionRequest(
    @field:Size(max = 100, message = "Description cannot exceed 100 characters")
    val description: String? = null
)

data class CancelTransactionRequest(
    @field:Size(max = 200, message = "Reason cannot exceed 200 characters")
    val reason: String? = null
)
