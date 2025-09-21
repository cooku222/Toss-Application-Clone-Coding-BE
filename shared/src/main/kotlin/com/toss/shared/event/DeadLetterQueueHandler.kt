package com.toss.shared.event

import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class DeadLetterQueueHandler(
    private val kafkaTemplate: KafkaTemplate<String, Any>
) {
    
    private val logger = LoggerFactory.getLogger(DeadLetterQueueHandler::class.java)
    
    fun sendToDeadLetterQueue(
        originalTopic: String,
        event: Event,
        error: Exception,
        retryCount: Int = 0
    ) {
        val dlqEvent = DeadLetterQueueEvent(
            originalTopic = originalTopic,
            originalEvent = event,
            error = error.message ?: "Unknown error",
            errorClass = error.javaClass.simpleName,
            retryCount = retryCount,
            timestamp = LocalDateTime.now()
        )
        
        try {
            kafkaTemplate.send("dlq-events", dlqEvent)
            logger.warn("Event sent to dead letter queue: ${event.eventType}, error: ${error.message}")
        } catch (e: Exception) {
            logger.error("Failed to send event to dead letter queue: ${event.eventType}", e)
        }
    }
    
    fun retryEvent(
        originalTopic: String,
        event: Event,
        maxRetries: Int = 3
    ): Boolean {
        val retryCount = getRetryCount(event)
        
        if (retryCount >= maxRetries) {
            logger.error("Max retries exceeded for event: ${event.eventType}, sending to DLQ")
            return false
        }
        
        try {
            // Add retry delay (exponential backoff)
            val delay = calculateRetryDelay(retryCount)
            Thread.sleep(delay)
            
            // Send to retry topic
            val retryEvent = event.copy(
                data = mapOf(
                    "originalEvent" to event,
                    "retryCount" to (retryCount + 1),
                    "retryTimestamp" to LocalDateTime.now()
                )
            )
            
            kafkaTemplate.send("retry-events", retryEvent)
            logger.info("Event retried: ${event.eventType}, retry count: ${retryCount + 1}")
            return true
            
        } catch (e: Exception) {
            logger.error("Failed to retry event: ${event.eventType}", e)
            return false
        }
    }
    
    private fun getRetryCount(event: Event): Int {
        return if (event.data is Map<*, *>) {
            val data = event.data as Map<*, *>
            data["retryCount"] as? Int ?: 0
        } else {
            0
        }
    }
    
    private fun calculateRetryDelay(retryCount: Int): Long {
        // Exponential backoff: 1s, 2s, 4s, 8s, etc.
        return (1000 * Math.pow(2.0, retryCount.toDouble())).toLong()
    }
}

data class DeadLetterQueueEvent(
    val originalTopic: String,
    val originalEvent: Event,
    val error: String,
    val errorClass: String,
    val retryCount: Int,
    val timestamp: LocalDateTime
)
