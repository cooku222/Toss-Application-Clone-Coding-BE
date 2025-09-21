package com.toss.notification.entity

import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@Entity
@Table(name = "notification_templates")
@EntityListeners(AuditingEntityListener::class)
data class NotificationTemplate(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    
    @Column(unique = true, nullable = false)
    val templateId: String,
    
    @Column(nullable = false)
    val name: String,
    
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
    
    @Column(nullable = false)
    val isActive: Boolean = true,
    
    @Column
    val description: String? = null,
    
    @Column
    val variables: String? = null, // JSON array of variable names
    
    @Column
    val version: Int = 1,
    
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
