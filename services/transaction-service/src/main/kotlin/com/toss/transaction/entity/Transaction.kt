package com.toss.transaction.entity

import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "transactions")
@EntityListeners(AuditingEntityListener::class)
data class Transaction(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    
    @Column(unique = true, nullable = false)
    val transactionId: String,
    
    @Column(nullable = false)
    val userId: Long,
    
    @Column(nullable = false)
    val fromAccountNumber: String,
    
    @Column(nullable = false)
    val toAccountNumber: String,
    
    @Column(nullable = false, precision = 19, scale = 2)
    val amount: BigDecimal,
    
    @Column
    val description: String? = null,
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val status: TransactionStatus = TransactionStatus.PENDING,
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val transactionType: TransactionType,
    
    @Column
    val failureReason: String? = null,
    
    @Column
    val idempotencyKey: String? = null,
    
    @Column
    val processedAt: LocalDateTime? = null,
    
    @Column
    val completedAt: LocalDateTime? = null,
    
    @Column
    val cancelledAt: LocalDateTime? = null,
    
    @CreatedDate
    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
    
    @LastModifiedDate
    @Column(nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now()
)

enum class TransactionStatus {
    PENDING, PROCESSING, COMPLETED, FAILED, CANCELLED, REFUNDED
}

enum class TransactionType {
    TRANSFER, PAYMENT, WITHDRAWAL, DEPOSIT, REFUND
}
