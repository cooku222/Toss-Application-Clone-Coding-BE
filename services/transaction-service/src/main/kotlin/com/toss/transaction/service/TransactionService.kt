package com.toss.transaction.service

import com.toss.transaction.dto.*
import com.toss.transaction.entity.Transaction
import com.toss.transaction.entity.TransactionStatus
import com.toss.transaction.entity.TransactionType
import com.toss.transaction.repository.TransactionRepository
import com.toss.shared.exception.ErrorCodes
import com.toss.shared.exception.TossException
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

@Service
@Transactional
class TransactionService(
    private val transactionRepository: TransactionRepository,
    private val redisTemplate: RedisTemplate<String, String>,
    private val kafkaTemplate: KafkaTemplate<String, Any>
) {
    
    fun createTransaction(userId: Long, request: CreateTransactionRequest): TransactionResponse {
        // Check idempotency
        val existingTransaction = transactionRepository.findByIdempotencyKey(request.idempotencyKey)
        if (existingTransaction != null) {
            return existingTransaction.toTransactionResponse()
        }
        
        // Generate transaction ID
        val transactionId = "TXN_${System.currentTimeMillis()}_${userId}"
        
        // Create transaction
        val transaction = Transaction(
            transactionId = transactionId,
            userId = userId,
            fromAccountNumber = request.fromAccountNumber,
            toAccountNumber = request.toAccountNumber,
            amount = request.amount,
            description = request.description,
            transactionType = request.transactionType,
            idempotencyKey = request.idempotencyKey,
            status = TransactionStatus.PENDING
        )
        
        val savedTransaction = transactionRepository.save(transaction)
        
        // Cache for idempotency
        redisTemplate.opsForValue().set(
            "transaction:${request.idempotencyKey}", 
            transactionId, 
            24, 
            TimeUnit.HOURS
        )
        
        // Publish transaction created event
        publishTransactionEvent("TRANSACTION_CREATED", savedTransaction)
        
        // Process transaction asynchronously
        processTransaction(savedTransaction)
        
        return savedTransaction.toTransactionResponse()
    }
    
    @Transactional(readOnly = true)
    fun getTransaction(transactionId: String, userId: Long): TransactionResponse {
        val transaction = transactionRepository.findByTransactionId(transactionId)
            ?: throw TossException(ErrorCodes.TRANSACTION_NOT_FOUND, "Transaction not found")
        
        if (transaction.userId != userId) {
            throw TossException(ErrorCodes.AUTH_ACCESS_DENIED, "Access denied")
        }
        
        return transaction.toTransactionResponse()
    }
    
    @Transactional(readOnly = true)
    fun getTransactions(
        userId: Long,
        page: Int = 0,
        size: Int = 20,
        status: TransactionStatus? = null,
        transactionType: TransactionType? = null
    ): Page<TransactionResponse> {
        val pageable: Pageable = PageRequest.of(page, size)
        
        val transactions = when {
            status != null -> transactionRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, status, pageable)
            transactionType != null -> transactionRepository.findByUserIdAndTransactionTypeOrderByCreatedAtDesc(userId, transactionType, pageable)
            else -> transactionRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
        }
        
        return transactions.map { it.toTransactionResponse() }
    }
    
    @Transactional(readOnly = true)
    fun getTransactionSummary(userId: Long): TransactionSummaryResponse {
        val now = LocalDateTime.now()
        val startOfDay = now.toLocalDate().atStartOfDay()
        val startOfMonth = now.toLocalDate().withDayOfMonth(1).atStartOfDay()
        
        val totalTransactions = transactionRepository.countTransactionsByUserAndStatusSince(
            userId, TransactionStatus.COMPLETED, LocalDateTime.of(2020, 1, 1, 0, 0)
        )
        
        val totalAmount = transactionRepository.getTotalAmountByUserAndStatusSince(
            userId, TransactionStatus.COMPLETED, LocalDateTime.of(2020, 1, 1, 0, 0)
        ) ?: BigDecimal.ZERO
        
        val pendingTransactions = transactionRepository.countTransactionsByUserAndStatusSince(
            userId, TransactionStatus.PENDING, startOfMonth
        )
        
        val completedTransactions = transactionRepository.countTransactionsByUserAndStatusSince(
            userId, TransactionStatus.COMPLETED, startOfMonth
        )
        
        val failedTransactions = transactionRepository.countTransactionsByUserAndStatusSince(
            userId, TransactionStatus.FAILED, startOfMonth
        )
        
        val todayTransactions = transactionRepository.countTransactionsByUserAndStatusSince(
            userId, TransactionStatus.COMPLETED, startOfDay
        )
        
        val todayAmount = transactionRepository.getTotalAmountByUserAndStatusSince(
            userId, TransactionStatus.COMPLETED, startOfDay
        ) ?: BigDecimal.ZERO
        
        return TransactionSummaryResponse(
            totalTransactions = totalTransactions,
            totalAmount = totalAmount,
            pendingTransactions = pendingTransactions,
            completedTransactions = completedTransactions,
            failedTransactions = failedTransactions,
            todayTransactions = todayTransactions,
            todayAmount = todayAmount
        )
    }
    
    fun updateTransaction(transactionId: String, userId: Long, request: UpdateTransactionRequest): TransactionResponse {
        val transaction = transactionRepository.findByTransactionId(transactionId)
            ?: throw TossException(ErrorCodes.TRANSACTION_NOT_FOUND, "Transaction not found")
        
        if (transaction.userId != userId) {
            throw TossException(ErrorCodes.AUTH_ACCESS_DENIED, "Access denied")
        }
        
        if (transaction.status != TransactionStatus.PENDING) {
            throw TossException(ErrorCodes.TRANSACTION_NOT_FOUND, "Transaction cannot be updated")
        }
        
        val updatedTransaction = transaction.copy(description = request.description)
        val savedTransaction = transactionRepository.save(updatedTransaction)
        
        return savedTransaction.toTransactionResponse()
    }
    
    fun cancelTransaction(transactionId: String, userId: Long, request: CancelTransactionRequest): TransactionResponse {
        val transaction = transactionRepository.findByTransactionId(transactionId)
            ?: throw TossException(ErrorCodes.TRANSACTION_NOT_FOUND, "Transaction not found")
        
        if (transaction.userId != userId) {
            throw TossException(ErrorCodes.AUTH_ACCESS_DENIED, "Access denied")
        }
        
        if (transaction.status !in listOf(TransactionStatus.PENDING, TransactionStatus.PROCESSING)) {
            throw TossException(ErrorCodes.TRANSACTION_NOT_FOUND, "Transaction cannot be cancelled")
        }
        
        val cancelledTransaction = transaction.copy(
            status = TransactionStatus.CANCELLED,
            failureReason = request.reason ?: "Cancelled by user",
            cancelledAt = LocalDateTime.now()
        )
        
        val savedTransaction = transactionRepository.save(cancelledTransaction)
        
        // Publish cancellation event
        publishTransactionEvent("TRANSACTION_CANCELLED", savedTransaction)
        
        return savedTransaction.toTransactionResponse()
    }
    
    private fun processTransaction(transaction: Transaction) {
        try {
            // Update status to processing
            val processingTransaction = transaction.copy(
                status = TransactionStatus.PROCESSING,
                processedAt = LocalDateTime.now()
            )
            transactionRepository.save(processingTransaction)
            
            // Simulate processing delay
            Thread.sleep(1000)
            
            // For demo purposes, randomly fail some transactions
            val shouldFail = (1..10).random() == 1
            
            if (shouldFail) {
                val failedTransaction = processingTransaction.copy(
                    status = TransactionStatus.FAILED,
                    failureReason = "Simulated processing failure",
                    completedAt = LocalDateTime.now()
                )
                transactionRepository.save(failedTransaction)
                publishTransactionEvent("TRANSACTION_FAILED", failedTransaction)
            } else {
                val completedTransaction = processingTransaction.copy(
                    status = TransactionStatus.COMPLETED,
                    completedAt = LocalDateTime.now()
                )
                transactionRepository.save(completedTransaction)
                publishTransactionEvent("TRANSACTION_COMPLETED", completedTransaction)
            }
            
        } catch (e: Exception) {
            val failedTransaction = transaction.copy(
                status = TransactionStatus.FAILED,
                failureReason = "Processing error: ${e.message}",
                completedAt = LocalDateTime.now()
            )
            transactionRepository.save(failedTransaction)
            publishTransactionEvent("TRANSACTION_FAILED", failedTransaction)
        }
    }
    
    private fun publishTransactionEvent(eventType: String, transaction: Transaction) {
        val event = mapOf(
            "eventType" to eventType,
            "transactionId" to transaction.transactionId,
            "userId" to transaction.userId,
            "fromAccount" to transaction.fromAccountNumber,
            "toAccount" to transaction.toAccountNumber,
            "amount" to transaction.amount,
            "status" to transaction.status.name,
            "timestamp" to LocalDateTime.now()
        )
        kafkaTemplate.send("transaction-events", event)
    }
}
