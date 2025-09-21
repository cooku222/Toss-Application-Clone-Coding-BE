package com.toss.notification.dto

import com.toss.notification.entity.NotificationChannel
import com.toss.notification.entity.NotificationType
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

data class CreateNotificationRequest(
    @field:NotNull(message = "User ID is required")
    val userId: Long,
    
    @field:NotBlank(message = "Title is required")
    @field:Size(max = 200, message = "Title cannot exceed 200 characters")
    val title: String,
    
    @field:NotBlank(message = "Content is required")
    @field:Size(max = 1000, message = "Content cannot exceed 1000 characters")
    val content: String,
    
    @field:NotNull(message = "Type is required")
    val type: NotificationType,
    
    @field:NotNull(message = "Channel is required")
    val channel: NotificationChannel,
    
    @field:NotBlank(message = "Recipient is required")
    val recipient: String,
    
    val templateId: String? = null,
    
    val templateData: String? = null,
    
    val scheduledAt: LocalDateTime? = null,
    
    val priority: Int = 0,
    
    val expiresAt: LocalDateTime? = null
)

data class SendNotificationRequest(
    @field:NotNull(message = "User ID is required")
    val userId: Long,
    
    @field:NotBlank(message = "Template ID is required")
    val templateId: String,
    
    @field:NotBlank(message = "Recipient is required")
    val recipient: String,
    
    val templateData: String? = null,
    
    val scheduledAt: LocalDateTime? = null,
    
    val priority: Int = 0
)

data class MarkAsReadRequest(
    @field:NotNull(message = "Notification ID is required")
    val notificationId: Long
)

data class BulkNotificationRequest(
    @field:NotNull(message = "User IDs are required")
    val userIds: List<Long>,
    
    @field:NotBlank(message = "Title is required")
    val title: String,
    
    @field:NotBlank(message = "Content is required")
    val content: String,
    
    @field:NotNull(message = "Type is required")
    val type: NotificationType,
    
    @field:NotNull(message = "Channel is required")
    val channel: NotificationChannel,
    
    val templateId: String? = null,
    
    val templateData: String? = null,
    
    val scheduledAt: LocalDateTime? = null,
    
    val priority: Int = 0
)
