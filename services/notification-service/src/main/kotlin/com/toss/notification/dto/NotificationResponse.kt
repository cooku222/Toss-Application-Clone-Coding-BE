package com.toss.notification.dto

import com.toss.notification.entity.Notification
import com.toss.notification.entity.NotificationChannel
import com.toss.notification.entity.NotificationStatus
import com.toss.notification.entity.NotificationType
import com.toss.shared.dto.PageResponse
import java.time.LocalDateTime

data class NotificationResponse(
    val id: Long,
    val userId: Long,
    val title: String,
    val content: String,
    val type: NotificationType,
    val channel: NotificationChannel,
    val status: NotificationStatus,
    val recipient: String,
    val templateId: String?,
    val scheduledAt: LocalDateTime?,
    val sentAt: LocalDateTime?,
    val deliveredAt: LocalDateTime?,
    val readAt: LocalDateTime?,
    val failureReason: String?,
    val retryCount: Int,
    val priority: Int,
    val expiresAt: LocalDateTime?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

data class NotificationSummaryResponse(
    val totalNotifications: Long,
    val unreadCount: Long,
    val pendingCount: Long,
    val sentCount: Long,
    val failedCount: Long,
    val todayCount: Long
)

data class NotificationStatsResponse(
    val period: String,
    val totalSent: Long,
    val totalDelivered: Long,
    val totalRead: Long,
    val deliveryRate: Double,
    val readRate: Double,
    val notifications: List<NotificationResponse>
)

// Extension functions for mapping
fun Notification.toNotificationResponse(): NotificationResponse {
    return NotificationResponse(
        id = this.id,
        userId = this.userId,
        title = this.title,
        content = this.content,
        type = this.type,
        channel = this.channel,
        status = this.status,
        recipient = this.recipient,
        templateId = this.templateId,
        scheduledAt = this.scheduledAt,
        sentAt = this.sentAt,
        deliveredAt = this.deliveredAt,
        readAt = this.readAt,
        failureReason = this.failureReason,
        retryCount = this.retryCount,
        priority = this.priority,
        expiresAt = this.expiresAt,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )
}
