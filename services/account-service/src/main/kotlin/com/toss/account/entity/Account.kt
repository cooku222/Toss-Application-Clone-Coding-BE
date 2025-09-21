package com.toss.account.entity

import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "accounts")
@EntityListeners(AuditingEntityListener::class)
data class Account(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    
    @Column(unique = true, nullable = false)
    val accountNumber: String,
    
    @Column(nullable = false)
    val userId: Long,
    
    @Column(nullable = false)
    val accountName: String,
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val accountType: AccountType,
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val status: AccountStatus = AccountStatus.ACTIVE,
    
    @Column(nullable = false, precision = 19, scale = 2)
    val balance: BigDecimal = BigDecimal.ZERO,
    
    @Column(nullable = false, precision = 19, scale = 2)
    val availableBalance: BigDecimal = BigDecimal.ZERO,
    
    @Column(nullable = false, precision = 19, scale = 2)
    val dailyLimit: BigDecimal = BigDecimal(1000000), // 1M KRW
    
    @Column(nullable = false, precision = 19, scale = 2)
    val monthlyLimit: BigDecimal = BigDecimal(10000000), // 10M KRW
    
    @Column
    val dailyUsedAmount: BigDecimal = BigDecimal.ZERO,
    
    @Column
    val monthlyUsedAmount: BigDecimal = BigDecimal.ZERO,
    
    @Column
    val lastUsedAt: LocalDateTime? = null,
    
    @CreatedDate
    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
    
    @LastModifiedDate
    @Column(nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now()
)

enum class AccountType {
    CHECKING, SAVINGS, INVESTMENT, LOAN
}

enum class AccountStatus {
    ACTIVE, INACTIVE, SUSPENDED, CLOSED
}
