package com.toss.ledger.entity

import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "account_balances")
@EntityListeners(AuditingEntityListener::class)
data class AccountBalance(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    
    @Column(nullable = false)
    val accountId: Long,
    
    @Column(nullable = false)
    val accountNumber: String,
    
    @Column(nullable = false, precision = 19, scale = 2)
    val balance: BigDecimal = BigDecimal.ZERO,
    
    @Column(nullable = false, precision = 19, scale = 2)
    val availableBalance: BigDecimal = BigDecimal.ZERO,
    
    @Column(nullable = false, precision = 19, scale = 2)
    val frozenAmount: BigDecimal = BigDecimal.ZERO,
    
    @Column
    val lastTransactionId: String? = null,
    
    @Column
    val lastTransactionAt: LocalDateTime? = null,
    
    @CreatedDate
    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
    
    @LastModifiedDate
    @Column(nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now()
)
