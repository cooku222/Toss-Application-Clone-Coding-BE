package com.toss.transaction.dto

import com.toss.transaction.entity.Transaction
import com.toss.transaction.entity.TransactionStatus
import com.toss.transaction.entity.TransactionType
import com.toss.shared.dto.PageResponse
import java.math.BigDecimal
import java.time.LocalDateTime

data class TransactionResponse(
    val id: Long,
    val transactionId: String,
    val fromAccountNumber: String,
    val toAccountNumber: String,
    val amount: BigDecimal,
    val description: String?,
    val status: TransactionStatus,
    val transactionType: TransactionType,
    val failureReason: String?,
    val processedAt: LocalDateTime?,
    val completedAt: LocalDateTime?,
    val cancelledAt: LocalDateTime?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

data class TransactionSummaryResponse(
    val totalTransactions: Long,
    val totalAmount: BigDecimal,
    val pendingTransactions: Long,
    val completedTransactions: Long,
    val failedTransactions: Long,
    val todayTransactions: Long,
    val todayAmount: BigDecimal
)

data class TransactionStatsResponse(
    val period: String,
    val totalCount: Long,
    val totalAmount: BigDecimal,
    val averageAmount: BigDecimal,
    val successRate: Double,
    val transactions: List<TransactionResponse>
)

// Extension functions for mapping
fun Transaction.toTransactionResponse(): TransactionResponse {
    return TransactionResponse(
        id = this.id,
        transactionId = this.transactionId,
        fromAccountNumber = this.fromAccountNumber,
        toAccountNumber = this.toAccountNumber,
        amount = this.amount,
        description = this.description,
        status = this.status,
        transactionType = this.transactionType,
        failureReason = this.failureReason,
        processedAt = this.processedAt,
        completedAt = this.completedAt,
        cancelledAt = this.cancelledAt,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )
}
