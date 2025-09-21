package com.toss.notification.repository

import com.toss.notification.entity.Notification
import com.toss.notification.entity.NotificationChannel
import com.toss.notification.entity.NotificationStatus
import com.toss.notification.entity.NotificationType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface NotificationRepository : JpaRepository<Notification, Long> {
    
    fun findByUserIdOrderByCreatedAtDesc(userId: Long, pageable: Pageable): Page<Notification>
    
    fun findByUserIdAndStatusOrderByCreatedAtDesc(
        userId: Long, 
        status: NotificationStatus, 
        pageable: Pageable
    ): Page<Notification>
    
    fun findByUserIdAndTypeOrderByCreatedAtDesc(
        userId: Long, 
        type: NotificationType, 
        pageable: Pageable
    ): Page<Notification>
    
    fun findByUserIdAndChannelOrderByCreatedAtDesc(
        userId: Long, 
        channel: NotificationChannel, 
        pageable: Pageable
    ): Page<Notification>
    
    fun findByStatusAndScheduledAtLessThanEqual(
        status: NotificationStatus, 
        scheduledAt: LocalDateTime
    ): List<Notification>
    
    fun findByStatusAndRetryCountLessThan(
        status: NotificationStatus, 
        maxRetries: Int
    ): List<Notification>
    
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.userId = :userId AND n.status = :status AND n.createdAt >= :startDate")
    fun countByUserIdAndStatusSince(
        @Param("userId") userId: Long,
        @Param("status") status: NotificationStatus,
        @Param("startDate") startDate: LocalDateTime
    ): Long
    
    @Query("SELECT n FROM Notification n WHERE n.status = :status AND n.expiresAt < :now")
    fun findExpiredNotifications(@Param("status") status: NotificationStatus, @Param("now") now: LocalDateTime): List<Notification>
    
    @Query("SELECT n FROM Notification n WHERE n.userId = :userId AND n.status = :status AND n.readAt IS NULL")
    fun findUnreadNotifications(@Param("userId") userId: Long, @Param("status") status: NotificationStatus): List<Notification>
}
