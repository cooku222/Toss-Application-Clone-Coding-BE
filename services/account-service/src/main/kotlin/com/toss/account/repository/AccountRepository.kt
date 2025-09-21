package com.toss.account.repository

import com.toss.account.entity.Account
import com.toss.account.entity.AccountStatus
import com.toss.account.entity.AccountType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDateTime
import jakarta.persistence.LockModeType

@Repository
interface AccountRepository : JpaRepository<Account, Long> {
    
    fun findByAccountNumber(accountNumber: String): Account?
    
    fun findByUserIdAndStatus(userId: Long, status: AccountStatus): List<Account>
    
    fun findByUserIdAndAccountTypeAndStatus(userId: Long, accountType: AccountType, status: AccountStatus): List<Account>
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.id = :id AND a.status = :status")
    fun findByIdWithLock(@Param("id") id: Long, @Param("status") status: AccountStatus): Account?
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.accountNumber = :accountNumber AND a.status = :status")
    fun findByAccountNumberWithLock(@Param("accountNumber") accountNumber: String, @Param("status") status: AccountStatus): Account?
    
    fun existsByAccountNumber(accountNumber: String): Boolean
    
    @Query("SELECT COUNT(a) FROM Account a WHERE a.userId = :userId AND a.status = :status")
    fun countByUserIdAndStatus(@Param("userId") userId: Long, @Param("status") status: AccountStatus): Long
    
    @Query("SELECT SUM(a.balance) FROM Account a WHERE a.userId = :userId AND a.status = :status")
    fun getTotalBalanceByUserId(@Param("userId") userId: Long, @Param("status") status: AccountStatus): BigDecimal?
    
    @Query("SELECT a FROM Account a WHERE a.status = :status AND a.lastUsedAt < :cutoffDate")
    fun findInactiveAccounts(@Param("status") status: AccountStatus, @Param("cutoffDate") cutoffDate: LocalDateTime): List<Account>
}
