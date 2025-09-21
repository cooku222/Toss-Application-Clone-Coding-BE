package com.toss.notification.repository

import com.toss.notification.entity.NotificationChannel
import com.toss.notification.entity.NotificationTemplate
import com.toss.notification.entity.NotificationType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface NotificationTemplateRepository : JpaRepository<NotificationTemplate, Long> {
    
    fun findByTemplateId(templateId: String): NotificationTemplate?
    
    fun findByTemplateIdAndIsActive(templateId: String, isActive: Boolean): NotificationTemplate?
    
    fun findByTypeAndChannelAndIsActive(
        type: NotificationType, 
        channel: NotificationChannel, 
        isActive: Boolean
    ): List<NotificationTemplate>
    
    fun existsByTemplateId(templateId: String): Boolean
}
