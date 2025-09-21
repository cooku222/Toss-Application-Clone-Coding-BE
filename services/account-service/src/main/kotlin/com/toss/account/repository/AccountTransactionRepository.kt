package com.toss.account.repository

import com.toss.account.entity.AccountTransaction
import com.toss.account.entity.TransactionType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDateTime

@Repository
interface AccountTransactionRepository : JpaRepository<AccountTransaction, Long> {
    
    fun findByAccountIdOrderByCreatedAtDesc(accountId: Long, pageable: Pageable): Page<AccountTransaction>
    
    fun findByTransactionId(transactionId: String): AccountTransaction?
    
    fun findByAccountIdAndTransactionTypeOrderByCreatedAtDesc(
        accountId: Long, 
        transactionType: TransactionType, 
        pageable: Pageable
    ): Page<AccountTransaction>
    
    fun findByAccountIdAndCreatedAtBetweenOrderByCreatedAtDesc(
        accountId: Long, 
        startDate: LocalDateTime, 
        endDate: LocalDateTime, 
        pageable: Pageable
    ): Page<AccountTransaction>
    
    @Query("SELECT SUM(at.amount) FROM AccountTransaction at WHERE at.accountId = :accountId AND at.transactionType = :transactionType AND at.createdAt >= :startDate")
    fun getTotalAmountByAccountAndTypeSince(
        @Param("accountId") accountId: Long,
        @Param("transactionType") transactionType: TransactionType,
        @Param("startDate") startDate: LocalDateTime
    ): BigDecimal?
    
    @Query("SELECT COUNT(at) FROM AccountTransaction at WHERE at.accountId = :accountId AND at.transactionType = :transactionType AND at.createdAt >= :startDate")
    fun countTransactionsByAccountAndTypeSince(
        @Param("accountId") accountId: Long,
        @Param("transactionType") transactionType: TransactionType,
        @Param("startDate") startDate: LocalDateTime
    ): Long
}
