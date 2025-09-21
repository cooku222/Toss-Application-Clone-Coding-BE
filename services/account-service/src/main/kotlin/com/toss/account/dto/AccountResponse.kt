package com.toss.account.dto

import com.toss.account.entity.Account
import com.toss.account.entity.AccountTransaction
import com.toss.account.entity.AccountType
import com.toss.account.entity.AccountStatus
import com.toss.account.entity.TransactionType
import com.toss.account.entity.TransactionStatus
import com.toss.shared.dto.PageResponse
import java.math.BigDecimal
import java.time.LocalDateTime

data class AccountResponse(
    val id: Long,
    val accountNumber: String,
    val accountName: String,
    val accountType: AccountType,
    val status: AccountStatus,
    val balance: BigDecimal,
    val availableBalance: BigDecimal,
    val dailyLimit: BigDecimal,
    val monthlyLimit: BigDecimal,
    val dailyUsedAmount: BigDecimal,
    val monthlyUsedAmount: BigDecimal,
    val lastUsedAt: LocalDateTime?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

data class AccountTransactionResponse(
    val id: Long,
    val transactionId: String,
    val transactionType: TransactionType,
    val amount: BigDecimal,
    val balanceAfter: BigDecimal,
    val description: String?,
    val referenceId: String?,
    val status: TransactionStatus,
    val createdAt: LocalDateTime
)

data class TransferResponse(
    val transactionId: String,
    val fromAccountNumber: String,
    val toAccountNumber: String,
    val amount: BigDecimal,
    val status: TransactionStatus,
    val processedAt: LocalDateTime
)

data class AccountSummaryResponse(
    val totalAccounts: Int,
    val totalBalance: BigDecimal,
    val availableBalance: BigDecimal,
    val accounts: List<AccountResponse>
)

// Extension functions for mapping
fun Account.toAccountResponse(): AccountResponse {
    return AccountResponse(
        id = this.id,
        accountNumber = this.accountNumber,
        accountName = this.accountName,
        accountType = this.accountType,
        status = this.status,
        balance = this.balance,
        availableBalance = this.availableBalance,
        dailyLimit = this.dailyLimit,
        monthlyLimit = this.monthlyLimit,
        dailyUsedAmount = this.dailyUsedAmount,
        monthlyUsedAmount = this.monthlyUsedAmount,
        lastUsedAt = this.lastUsedAt,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )
}

fun AccountTransaction.toAccountTransactionResponse(): AccountTransactionResponse {
    return AccountTransactionResponse(
        id = this.id,
        transactionId = this.transactionId,
        transactionType = this.transactionType,
        amount = this.amount,
        balanceAfter = this.balanceAfter,
        description = this.description,
        referenceId = this.referenceId,
        status = this.status,
        createdAt = this.createdAt
    )
}
