package com.toss.ledger.repository

import com.toss.ledger.entity.AccountBalance
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import jakarta.persistence.LockModeType

@Repository
interface AccountBalanceRepository : JpaRepository<AccountBalance, Long> {
    
    fun findByAccountId(accountId: Long): AccountBalance?
    
    fun findByAccountNumber(accountNumber: String): AccountBalance?
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ab FROM AccountBalance ab WHERE ab.accountId = :accountId")
    fun findByAccountIdWithLock(@Param("accountId") accountId: Long): AccountBalance?
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ab FROM AccountBalance ab WHERE ab.accountNumber = :accountNumber")
    fun findByAccountNumberWithLock(@Param("accountNumber") accountNumber: String): AccountBalance?
    
    fun existsByAccountId(accountId: Long): Boolean
    
    fun existsByAccountNumber(accountNumber: String): Boolean
}
