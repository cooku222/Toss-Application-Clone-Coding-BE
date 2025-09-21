package com.toss.account.entity

import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "account_transactions")
@EntityListeners(AuditingEntityListener::class)
data class AccountTransaction(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    
    @Column(nullable = false)
    val accountId: Long,
    
    @Column(nullable = false)
    val transactionId: String,
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val transactionType: TransactionType,
    
    @Column(nullable = false, precision = 19, scale = 2)
    val amount: BigDecimal,
    
    @Column(nullable = false, precision = 19, scale = 2)
    val balanceAfter: BigDecimal,
    
    @Column
    val description: String? = null,
    
    @Column
    val referenceId: String? = null,
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val status: TransactionStatus = TransactionStatus.COMPLETED,
    
    @CreatedDate
    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)

enum class TransactionType {
    DEPOSIT, WITHDRAWAL, TRANSFER_IN, TRANSFER_OUT, FEE, INTEREST, REFUND
}

enum class TransactionStatus {
    PENDING, COMPLETED, FAILED, CANCELLED
}
