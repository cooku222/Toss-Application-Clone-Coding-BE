package com.toss.ledger.repository

import com.toss.ledger.entity.LedgerEntry
import com.toss.ledger.entity.LedgerEntryStatus
import com.toss.ledger.entity.LedgerEntryType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDateTime

@Repository
interface LedgerEntryRepository : JpaRepository<LedgerEntry, Long> {
    
    fun findByTransactionId(transactionId: String): List<LedgerEntry>
    
    fun findByAccountIdOrderByCreatedAtDesc(accountId: Long, pageable: Pageable): Page<LedgerEntry>
    
    fun findByAccountIdAndEntryTypeOrderByCreatedAtDesc(
        accountId: Long, 
        entryType: LedgerEntryType, 
        pageable: Pageable
    ): Page<LedgerEntry>
    
    fun findByAccountIdAndStatusOrderByCreatedAtDesc(
        accountId: Long, 
        status: LedgerEntryStatus, 
        pageable: Pageable
    ): Page<LedgerEntry>
    
    fun findByAccountIdAndCreatedAtBetweenOrderByCreatedAtDesc(
        accountId: Long, 
        startDate: LocalDateTime, 
        endDate: LocalDateTime, 
        pageable: Pageable
    ): Page<LedgerEntry>
    
    @Query("SELECT SUM(le.amount) FROM LedgerEntry le WHERE le.accountId = :accountId AND le.entryType = :entryType AND le.status = :status")
    fun getTotalAmountByAccountAndTypeAndStatus(
        @Param("accountId") accountId: Long,
        @Param("entryType") entryType: LedgerEntryType,
        @Param("status") status: LedgerEntryStatus
    ): BigDecimal?
    
    @Query("SELECT COUNT(le) FROM LedgerEntry le WHERE le.accountId = :accountId AND le.entryType = :entryType AND le.status = :status AND le.createdAt >= :startDate")
    fun countEntriesByAccountAndTypeAndStatusSince(
        @Param("accountId") accountId: Long,
        @Param("entryType") entryType: LedgerEntryType,
        @Param("status") status: LedgerEntryStatus,
        @Param("startDate") startDate: LocalDateTime
    ): Long
    
    @Query("SELECT le FROM LedgerEntry le WHERE le.status = :status AND le.createdAt < :cutoffDate")
    fun findOldEntries(@Param("status") status: LedgerEntryStatus, @Param("cutoffDate") cutoffDate: LocalDateTime): List<LedgerEntry>
}
