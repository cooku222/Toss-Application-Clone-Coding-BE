package com.toss.transaction.repository

import com.toss.transaction.entity.Transaction
import com.toss.transaction.entity.TransactionStatus
import com.toss.transaction.entity.TransactionType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface TransactionRepository : JpaRepository<Transaction, Long> {
    
    fun findByTransactionId(transactionId: String): Transaction?
    
    fun findByUserIdOrderByCreatedAtDesc(userId: Long, pageable: Pageable): Page<Transaction>
    
    fun findByUserIdAndStatusOrderByCreatedAtDesc(
        userId: Long, 
        status: TransactionStatus, 
        pageable: Pageable
    ): Page<Transaction>
    
    fun findByUserIdAndTransactionTypeOrderByCreatedAtDesc(
        userId: Long, 
        transactionType: TransactionType, 
        pageable: Pageable
    ): Page<Transaction>
    
    fun findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(
        userId: Long, 
        startDate: LocalDateTime, 
        endDate: LocalDateTime, 
        pageable: Pageable
    ): Page<Transaction>
    
    fun findByIdempotencyKey(idempotencyKey: String): Transaction?
    
    @Query("SELECT t FROM Transaction t WHERE t.status = :status AND t.createdAt < :cutoffDate")
    fun findStuckTransactions(@Param("status") status: TransactionStatus, @Param("cutoffDate") cutoffDate: LocalDateTime): List<Transaction>
    
    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.userId = :userId AND t.status = :status AND t.createdAt >= :startDate")
    fun countTransactionsByUserAndStatusSince(
        @Param("userId") userId: Long,
        @Param("status") status: TransactionStatus,
        @Param("startDate") startDate: LocalDateTime
    ): Long
    
    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.userId = :userId AND t.status = :status AND t.createdAt >= :startDate")
    fun getTotalAmountByUserAndStatusSince(
        @Param("userId") userId: Long,
        @Param("status") status: TransactionStatus,
        @Param("startDate") startDate: LocalDateTime
    ): java.math.BigDecimal?
}
