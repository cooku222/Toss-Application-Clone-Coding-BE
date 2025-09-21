package com.toss.ledger.dto

import com.toss.ledger.entity.LedgerEntryType
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.math.BigDecimal

data class CreateLedgerEntryRequest(
    @field:NotBlank(message = "Transaction ID is required")
    val transactionId: String,
    
    @field:NotNull(message = "Account ID is required")
    val accountId: Long,
    
    @field:NotBlank(message = "Account number is required")
    val accountNumber: String,
    
    @field:NotNull(message = "Entry type is required")
    val entryType: LedgerEntryType,
    
    @field:NotNull(message = "Amount is required")
    @field:DecimalMin(value = "0.01", message = "Amount must be positive")
    val amount: BigDecimal,
    
    @field:Size(max = 200, message = "Description cannot exceed 200 characters")
    val description: String? = null,
    
    @field:Size(max = 100, message = "Reference ID cannot exceed 100 characters")
    val referenceId: String? = null
)

data class ReverseLedgerEntryRequest(
    @field:Size(max = 200, message = "Reason cannot exceed 200 characters")
    val reason: String? = null
)

data class BalanceAdjustmentRequest(
    @field:NotNull(message = "Account ID is required")
    val accountId: Long,
    
    @field:NotBlank(message = "Account number is required")
    val accountNumber: String,
    
    @field:NotNull(message = "Amount is required")
    val amount: BigDecimal,
    
    @field:Size(max = 200, message = "Description cannot exceed 200 characters")
    val description: String? = null,
    
    @field:NotBlank(message = "Transaction ID is required")
    val transactionId: String
)
