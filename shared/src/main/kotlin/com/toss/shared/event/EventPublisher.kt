package com.toss.shared.event

import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class EventPublisher(
    private val kafkaTemplate: KafkaTemplate<String, Any>
) {
    
    fun publishEvent(topic: String, eventType: String, data: Any) {
        val event = Event(
            eventType = eventType,
            data = data,
            timestamp = LocalDateTime.now(),
            version = "1.0"
        )
        
        kafkaTemplate.send(topic, event)
    }
    
    fun publishEvent(topic: String, event: Event) {
        kafkaTemplate.send(topic, event)
    }
    
    fun publishUserEvent(eventType: String, data: Any) {
        publishEvent("user-events", eventType, data)
    }
    
    fun publishAccountEvent(eventType: String, data: Any) {
        publishEvent("account-events", eventType, data)
    }
    
    fun publishTransactionEvent(eventType: String, data: Any) {
        publishEvent("transaction-events", eventType, data)
    }
    
    fun publishLedgerEvent(eventType: String, data: Any) {
        publishEvent("ledger-events", eventType, data)
    }
    
    fun publishNotificationEvent(eventType: String, data: Any) {
        publishEvent("notification-events", eventType, data)
    }
    
    fun publishTransferEvent(eventType: String, data: Any) {
        publishEvent("transfer-events", eventType, data)
    }
}

data class Event(
    val eventType: String,
    val data: Any,
    val timestamp: LocalDateTime,
    val version: String,
    val correlationId: String? = null,
    val source: String? = null
)
