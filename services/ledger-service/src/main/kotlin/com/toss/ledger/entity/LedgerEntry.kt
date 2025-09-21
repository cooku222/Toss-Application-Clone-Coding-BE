package com.toss.ledger.entity

import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "ledger_entries")
@EntityListeners(AuditingEntityListener::class)
data class LedgerEntry(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    
    @Column(nullable = false)
    val transactionId: String,
    
    @Column(nullable = false)
    val accountId: Long,
    
    @Column(nullable = false)
    val accountNumber: String,
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val entryType: LedgerEntryType,
    
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
    val status: LedgerEntryStatus = LedgerEntryStatus.ACTIVE,
    
    @Column
    val reversedAt: LocalDateTime? = null,
    
    @Column
    val reversedBy: String? = null,
    
    @Column
    val reversalReason: String? = null,
    
    @CreatedDate
    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)

enum class LedgerEntryType {
    DEBIT, CREDIT
}

enum class LedgerEntryStatus {
    ACTIVE, REVERSED, CANCELLED
}
