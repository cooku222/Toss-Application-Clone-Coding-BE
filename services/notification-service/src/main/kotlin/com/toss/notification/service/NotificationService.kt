package com.toss.notification.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.toss.notification.dto.*
import com.toss.notification.entity.*
import com.toss.notification.repository.NotificationRepository
import com.toss.notification.repository.NotificationTemplateRepository
import com.toss.shared.exception.ErrorCodes
import com.toss.shared.exception.TossException
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional
class NotificationService(
    private val notificationRepository: NotificationRepository,
    private val templateRepository: NotificationTemplateRepository,
    private val kafkaTemplate: KafkaTemplate<String, Any>,
    private val mailSender: JavaMailSender,
    private val objectMapper: ObjectMapper
) {
    
    fun createNotification(request: CreateNotificationRequest): NotificationResponse {
        val notification = Notification(
            userId = request.userId,
            title = request.title,
            content = request.content,
            type = request.type,
            channel = request.channel,
            recipient = request.recipient,
            templateId = request.templateId,
            templateData = request.templateData,
            scheduledAt = request.scheduledAt,
            priority = request.priority,
            expiresAt = request.expiresAt
        )
        
        val savedNotification = notificationRepository.save(notification)
        
        // Send immediately if not scheduled
        if (request.scheduledAt == null) {
            sendNotification(savedNotification)
        }
        
        // Publish notification created event
        publishNotificationEvent("NOTIFICATION_CREATED", savedNotification)
        
        return savedNotification.toNotificationResponse()
    }
    
    fun sendNotificationFromTemplate(request: SendNotificationRequest): NotificationResponse {
        val template = templateRepository.findByTemplateIdAndIsActive(request.templateId, true)
            ?: throw TossException(ErrorCodes.NOTIFICATION_TEMPLATE_NOT_FOUND, "Template not found")
        
        // Process template with data
        val processedTitle = processTemplate(template.title, request.templateData)
        val processedContent = processTemplate(template.content, request.templateData)
        
        val notification = Notification(
            userId = request.userId,
            title = processedTitle,
            content = processedContent,
            type = template.type,
            channel = template.channel,
            recipient = request.recipient,
            templateId = request.templateId,
            templateData = request.templateData,
            scheduledAt = request.scheduledAt,
            priority = request.priority
        )
        
        val savedNotification = notificationRepository.save(notification)
        
        // Send immediately if not scheduled
        if (request.scheduledAt == null) {
            sendNotification(savedNotification)
        }
        
        return savedNotification.toNotificationResponse()
    }
    
    @Transactional(readOnly = true)
    fun getNotifications(
        userId: Long,
        page: Int = 0,
        size: Int = 20,
        status: NotificationStatus? = null,
        type: NotificationType? = null,
        channel: NotificationChannel? = null
    ): Page<NotificationResponse> {
        val pageable: Pageable = PageRequest.of(page, size)
        
        val notifications = when {
            status != null -> notificationRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, status, pageable)
            type != null -> notificationRepository.findByUserIdAndTypeOrderByCreatedAtDesc(userId, type, pageable)
            channel != null -> notificationRepository.findByUserIdAndChannelOrderByCreatedAtDesc(userId, channel, pageable)
            else -> notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
        }
        
        return notifications.map { it.toNotificationResponse() }
    }
    
    @Transactional(readOnly = true)
    fun getNotificationSummary(userId: Long): NotificationSummaryResponse {
        val now = LocalDateTime.now()
        val startOfDay = now.toLocalDate().atStartOfDay()
        
        val totalNotifications = notificationRepository.countByUserIdAndStatusSince(
            userId, NotificationStatus.SENT, LocalDateTime.of(2020, 1, 1, 0, 0)
        )
        
        val unreadCount = notificationRepository.findUnreadNotifications(userId, NotificationStatus.DELIVERED).size.toLong()
        
        val pendingCount = notificationRepository.countByUserIdAndStatusSince(
            userId, NotificationStatus.PENDING, startOfDay
        )
        
        val sentCount = notificationRepository.countByUserIdAndStatusSince(
            userId, NotificationStatus.SENT, startOfDay
        )
        
        val failedCount = notificationRepository.countByUserIdAndStatusSince(
            userId, NotificationStatus.FAILED, startOfDay
        )
        
        val todayCount = notificationRepository.countByUserIdAndStatusSince(
            userId, NotificationStatus.SENT, startOfDay
        )
        
        return NotificationSummaryResponse(
            totalNotifications = totalNotifications,
            unreadCount = unreadCount,
            pendingCount = pendingCount,
            sentCount = sentCount,
            failedCount = failedCount,
            todayCount = todayCount
        )
    }
    
    fun markAsRead(notificationId: Long, userId: Long): NotificationResponse {
        val notification = notificationRepository.findById(notificationId)
            .orElseThrow { TossException(ErrorCodes.NOTIFICATION_SEND_FAILED, "Notification not found") }
        
        if (notification.userId != userId) {
            throw TossException(ErrorCodes.AUTH_ACCESS_DENIED, "Access denied")
        }
        
        val updatedNotification = notification.copy(
            readAt = LocalDateTime.now()
        )
        
        val savedNotification = notificationRepository.save(updatedNotification)
        
        // Publish read event
        publishNotificationEvent("NOTIFICATION_READ", savedNotification)
        
        return savedNotification.toNotificationResponse()
    }
    
    fun sendBulkNotification(request: BulkNotificationRequest): List<NotificationResponse> {
        val notifications = request.userIds.map { userId ->
            Notification(
                userId = userId,
                title = request.title,
                content = request.content,
                type = request.type,
                channel = request.channel,
                recipient = "", // Will be filled based on user preferences
                templateId = request.templateId,
                templateData = request.templateData,
                scheduledAt = request.scheduledAt,
                priority = request.priority
            )
        }
        
        val savedNotifications = notificationRepository.saveAll(notifications)
        
        // Send immediately if not scheduled
        if (request.scheduledAt == null) {
            savedNotifications.forEach { sendNotification(it) }
        }
        
        // Publish bulk notification event
        publishNotificationEvent("BULK_NOTIFICATION_SENT", savedNotifications)
        
        return savedNotifications.map { it.toNotificationResponse() }
    }
    
    private fun sendNotification(notification: Notification) {
        try {
            when (notification.channel) {
                NotificationChannel.EMAIL -> sendEmail(notification)
                NotificationChannel.SMS -> sendSms(notification)
                NotificationChannel.PUSH -> sendPush(notification)
                NotificationChannel.IN_APP -> sendInApp(notification)
            }
            
            val updatedNotification = notification.copy(
                status = NotificationStatus.SENT,
                sentAt = LocalDateTime.now()
            )
            notificationRepository.save(updatedNotification)
            
        } catch (e: Exception) {
            val updatedNotification = notification.copy(
                status = NotificationStatus.FAILED,
                failureReason = e.message,
                retryCount = notification.retryCount + 1
            )
            notificationRepository.save(updatedNotification)
            
            // Retry if under max retries
            if (notification.retryCount < notification.maxRetries) {
                // Schedule retry (implement retry logic)
                scheduleRetry(updatedNotification)
            }
        }
    }
    
    private fun sendEmail(notification: Notification) {
        val message = SimpleMailMessage()
        message.setTo(notification.recipient)
        message.setSubject(notification.title)
        message.setText(notification.content)
        
        mailSender.send(message)
    }
    
    private fun sendSms(notification: Notification) {
        // Implement SMS sending (Twilio, etc.)
        // For demo purposes, just log
        println("SMS sent to ${notification.recipient}: ${notification.content}")
    }
    
    private fun sendPush(notification: Notification) {
        // Implement push notification (FCM, etc.)
        // For demo purposes, just log
        println("Push notification sent to ${notification.recipient}: ${notification.title}")
    }
    
    private fun sendInApp(notification: Notification) {
        // In-app notifications are already stored in database
        // Just mark as delivered
        val updatedNotification = notification.copy(
            status = NotificationStatus.DELIVERED,
            deliveredAt = LocalDateTime.now()
        )
        notificationRepository.save(updatedNotification)
    }
    
    private fun processTemplate(template: String, data: String?): String {
        if (data == null) return template
        
        try {
            val templateData = objectMapper.readValue(data, Map::class.java)
            var processedTemplate = template
            
            templateData.forEach { (key, value) ->
                processedTemplate = processedTemplate.replace("{{$key}}", value.toString())
            }
            
            return processedTemplate
        } catch (e: Exception) {
            return template
        }
    }
    
    private fun scheduleRetry(notification: Notification) {
        // Implement retry scheduling logic
        // For demo purposes, just log
        println("Scheduling retry for notification ${notification.id}")
    }
    
    private fun publishNotificationEvent(eventType: String, data: Any) {
        val event = mapOf(
            "eventType" to eventType,
            "data" to data,
            "timestamp" to LocalDateTime.now()
        )
        kafkaTemplate.send("notification-events", event)
    }
}
