package com.toss.notification.controller

import com.toss.notification.dto.*
import com.toss.notification.entity.NotificationChannel
import com.toss.notification.entity.NotificationStatus
import com.toss.notification.entity.NotificationType
import com.toss.notification.service.NotificationService
import com.toss.shared.dto.ApiResponse
import com.toss.shared.dto.PageResponse
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/notifications")
class NotificationController(
    private val notificationService: NotificationService
) {
    
    @PostMapping
    fun createNotification(
        @Valid @RequestBody request: CreateNotificationRequest
    ): ResponseEntity<ApiResponse<NotificationResponse>> {
        val notification = notificationService.createNotification(request)
        return ResponseEntity.ok(ApiResponse.success(notification))
    }
    
    @PostMapping("/send")
    fun sendNotificationFromTemplate(
        @Valid @RequestBody request: SendNotificationRequest
    ): ResponseEntity<ApiResponse<NotificationResponse>> {
        val notification = notificationService.sendNotificationFromTemplate(request)
        return ResponseEntity.ok(ApiResponse.success(notification))
    }
    
    @GetMapping
    fun getNotifications(
        authentication: Authentication,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) status: NotificationStatus?,
        @RequestParam(required = false) type: NotificationType?,
        @RequestParam(required = false) channel: NotificationChannel?
    ): ResponseEntity<ApiResponse<PageResponse<NotificationResponse>>> {
        val userId = authentication.name.toLong()
        val notifications = notificationService.getNotifications(userId, page, size, status, type, channel)
        val response = PageResponse(
            content = notifications.content,
            page = notifications.number,
            size = notifications.size,
            totalElements = notifications.totalElements,
            totalPages = notifications.totalPages,
            first = notifications.isFirst,
            last = notifications.isLast
        )
        return ResponseEntity.ok(ApiResponse.success(response))
    }
    
    @GetMapping("/summary")
    fun getNotificationSummary(authentication: Authentication): ResponseEntity<ApiResponse<NotificationSummaryResponse>> {
        val userId = authentication.name.toLong()
        val summary = notificationService.getNotificationSummary(userId)
        return ResponseEntity.ok(ApiResponse.success(summary))
    }
    
    @PostMapping("/{notificationId}/read")
    fun markAsRead(
        authentication: Authentication,
        @PathVariable notificationId: Long
    ): ResponseEntity<ApiResponse<NotificationResponse>> {
        val userId = authentication.name.toLong()
        val notification = notificationService.markAsRead(notificationId, userId)
        return ResponseEntity.ok(ApiResponse.success(notification))
    }
    
    @PostMapping("/bulk")
    fun sendBulkNotification(
        @Valid @RequestBody request: BulkNotificationRequest
    ): ResponseEntity<ApiResponse<List<NotificationResponse>>> {
        val notifications = notificationService.sendBulkNotification(request)
        return ResponseEntity.ok(ApiResponse.success(notifications))
    }
}
