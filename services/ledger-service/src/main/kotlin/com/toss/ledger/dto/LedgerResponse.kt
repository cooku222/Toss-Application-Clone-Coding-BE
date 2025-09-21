package com.toss.ledger.dto

import com.toss.ledger.entity.AccountBalance
import com.toss.ledger.entity.LedgerEntry
import com.toss.ledger.entity.LedgerEntryStatus
import com.toss.ledger.entity.LedgerEntryType
import com.toss.shared.dto.PageResponse
import java.math.BigDecimal
import java.time.LocalDateTime

data class LedgerEntryResponse(
    val id: Long,
    val transactionId: String,
    val accountId: Long,
    val accountNumber: String,
    val entryType: LedgerEntryType,
    val amount: BigDecimal,
    val balanceAfter: BigDecimal,
    val description: String?,
    val referenceId: String?,
    val status: LedgerEntryStatus,
    val reversedAt: LocalDateTime?,
    val reversedBy: String?,
    val reversalReason: String?,
    val createdAt: LocalDateTime
)

data class AccountBalanceResponse(
    val id: Long,
    val accountId: Long,
    val accountNumber: String,
    val balance: BigDecimal,
    val availableBalance: BigDecimal,
    val frozenAmount: BigDecimal,
    val lastTransactionId: String?,
    val lastTransactionAt: LocalDateTime?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

data class LedgerSummaryResponse(
    val totalEntries: Long,
    val totalDebits: BigDecimal,
    val totalCredits: BigDecimal,
    val netBalance: BigDecimal,
    val activeEntries: Long,
    val reversedEntries: Long
)

data class BalanceReconciliationResponse(
    val accountId: Long,
    val accountNumber: String,
    val ledgerBalance: BigDecimal,
    val accountBalance: BigDecimal,
    val difference: BigDecimal,
    val isReconciled: Boolean,
    val lastReconciliationAt: LocalDateTime
)

// Extension functions for mapping
fun LedgerEntry.toLedgerEntryResponse(): LedgerEntryResponse {
    return LedgerEntryResponse(
        id = this.id,
        transactionId = this.transactionId,
        accountId = this.accountId,
        accountNumber = this.accountNumber,
        entryType = this.entryType,
        amount = this.amount,
        balanceAfter = this.balanceAfter,
        description = this.description,
        referenceId = this.referenceId,
        status = this.status,
        reversedAt = this.reversedAt,
        reversedBy = this.reversedBy,
        reversalReason = this.reversalReason,
        createdAt = this.createdAt
    )
}

fun AccountBalance.toAccountBalanceResponse(): AccountBalanceResponse {
    return AccountBalanceResponse(
        id = this.id,
        accountId = this.accountId,
        accountNumber = this.accountNumber,
        balance = this.balance,
        availableBalance = this.availableBalance,
        frozenAmount = this.frozenAmount,
        lastTransactionId = this.lastTransactionId,
        lastTransactionAt = this.lastTransactionAt,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )
}
