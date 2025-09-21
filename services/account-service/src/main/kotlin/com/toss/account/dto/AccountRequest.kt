package com.toss.account.dto

import com.toss.account.entity.AccountType
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.math.BigDecimal

data class CreateAccountRequest(
    @field:NotBlank(message = "Account name is required")
    @field:Size(min = 2, max = 50, message = "Account name must be between 2 and 50 characters")
    val accountName: String,
    
    @field:NotNull(message = "Account type is required")
    val accountType: AccountType,
    
    @field:DecimalMin(value = "0.0", inclusive = false, message = "Initial balance must be positive")
    @field:DecimalMax(value = "100000000.0", message = "Initial balance cannot exceed 100M")
    val initialBalance: BigDecimal = BigDecimal.ZERO
)

data class UpdateAccountRequest(
    @field:Size(min = 2, max = 50, message = "Account name must be between 2 and 50 characters")
    val accountName: String? = null,
    
    @field:DecimalMin(value = "0.0", inclusive = false, message = "Daily limit must be positive")
    @field:DecimalMax(value = "100000000.0", message = "Daily limit cannot exceed 100M")
    val dailyLimit: BigDecimal? = null,
    
    @field:DecimalMin(value = "0.0", inclusive = false, message = "Monthly limit must be positive")
    @field:DecimalMax(value = "1000000000.0", message = "Monthly limit cannot exceed 1B")
    val monthlyLimit: BigDecimal? = null
)

data class TransferRequest(
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
    
    @field:NotBlank(message = "Idempotency key is required")
    val idempotencyKey: String
)
