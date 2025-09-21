package com.toss.shared.security

import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class SecurityAuditService(
    private val kafkaTemplate: KafkaTemplate<String, Any>
) {
    
    fun logSecurityEvent(
        eventType: SecurityEventType,
        userId: Long?,
        ipAddress: String,
        userAgent: String?,
        details: Map<String, Any> = emptyMap()
    ) {
        val auditEvent = SecurityAuditEvent(
            eventType = eventType,
            userId = userId,
            ipAddress = ipAddress,
            userAgent = userAgent,
            details = details,
            timestamp = LocalDateTime.now()
        )
        
        kafkaTemplate.send("security-audit-events", auditEvent)
    }
    
    fun logAuthenticationEvent(userId: Long, ipAddress: String, success: Boolean, details: Map<String, Any> = emptyMap()) {
        val eventType = if (success) SecurityEventType.LOGIN_SUCCESS else SecurityEventType.LOGIN_FAILURE
        logSecurityEvent(eventType, userId, ipAddress, null, details)
    }
    
    fun logAuthorizationEvent(userId: Long, resource: String, action: String, success: Boolean, ipAddress: String) {
        val eventType = if (success) SecurityEventType.ACCESS_GRANTED else SecurityEventType.ACCESS_DENIED
        logSecurityEvent(eventType, userId, ipAddress, null, mapOf(
            "resource" to resource,
            "action" to action
        ))
    }
    
    fun logSuspiciousActivity(userId: Long?, ipAddress: String, activity: String, details: Map<String, Any> = emptyMap()) {
        logSecurityEvent(SecurityEventType.SUSPICIOUS_ACTIVITY, userId, ipAddress, null, details + mapOf(
            "activity" to activity
        ))
    }
}

enum class SecurityEventType {
    LOGIN_SUCCESS,
    LOGIN_FAILURE,
    LOGOUT,
    ACCESS_GRANTED,
    ACCESS_DENIED,
    PASSWORD_CHANGE,
    ACCOUNT_LOCKED,
    ACCOUNT_UNLOCKED,
    SUSPICIOUS_ACTIVITY,
    RATE_LIMIT_EXCEEDED,
    INVALID_TOKEN,
    TOKEN_EXPIRED
}

data class SecurityAuditEvent(
    val eventType: SecurityEventType,
    val userId: Long?,
    val ipAddress: String,
    val userAgent: String?,
    val details: Map<String, Any>,
    val timestamp: LocalDateTime
)
