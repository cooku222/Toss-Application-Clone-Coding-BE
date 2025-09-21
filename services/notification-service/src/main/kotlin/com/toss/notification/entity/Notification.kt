package com.toss.notification.entity

import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@Entity
@Table(name = "notifications")
@EntityListeners(AuditingEntityListener::class)
data class Notification(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    
    @Column(nullable = false)
    val userId: Long,
    
    @Column(nullable = false)
    val title: String,
    
    @Column(nullable = false, columnDefinition = "TEXT")
    val content: String,
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val type: NotificationType,
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val channel: NotificationChannel,
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val status: NotificationStatus = NotificationStatus.PENDING,
    
    @Column
    val recipient: String, // Email, phone number, or device token
    
    @Column
    val templateId: String? = null,
    
    @Column
    val templateData: String? = null, // JSON data for template
    
    @Column
    val scheduledAt: LocalDateTime? = null,
    
    @Column
    val sentAt: LocalDateTime? = null,
    
    @Column
    val deliveredAt: LocalDateTime? = null,
    
    @Column
    val readAt: LocalDateTime? = null,
    
    @Column
    val failureReason: String? = null,
    
    @Column
    val retryCount: Int = 0,
    
    @Column
    val maxRetries: Int = 3,
    
    @Column
    val priority: Int = 0, // Higher number = higher priority
    
    @Column
    val expiresAt: LocalDateTime? = null,
    
    @CreatedDate
    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
    
    @LastModifiedDate
    @Column(nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now()
)

enum class NotificationType {
    TRANSACTION, SECURITY, MARKETING, SYSTEM, PROMOTION
}

enum class NotificationChannel {
    EMAIL, SMS, PUSH, IN_APP
}

enum class NotificationStatus {
    PENDING, SENT, DELIVERED, READ, FAILED, CANCELLED, EXPIRED
}
