package com.toss.shared.event

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component

@Component
abstract class EventConsumer(
    protected val objectMapper: ObjectMapper
) {
    
    protected val logger = LoggerFactory.getLogger(this::class.java)
    
    @KafkaListener(topics = ["user-events"], groupId = "toss-consumer")
    fun handleUserEvent(
        @Payload event: Event,
        @Header(KafkaHeaders.RECEIVED_TOPIC) topic: String,
        @Header(KafkaHeaders.RECEIVED_PARTITION_ID) partition: Int,
        @Header(KafkaHeaders.OFFSET) offset: Long,
        acknowledgment: Acknowledgment
    ) {
        try {
            logger.info("Received user event: ${event.eventType} from topic: $topic, partition: $partition, offset: $offset")
            onUserEvent(event)
            acknowledgment.acknowledge()
        } catch (e: Exception) {
            logger.error("Error processing user event: ${event.eventType}", e)
            // Implement retry logic or dead letter queue
            handleError(event, e)
        }
    }
    
    @KafkaListener(topics = ["account-events"], groupId = "toss-consumer")
    fun handleAccountEvent(
        @Payload event: Event,
        @Header(KafkaHeaders.RECEIVED_TOPIC) topic: String,
        @Header(KafkaHeaders.RECEIVED_PARTITION_ID) partition: Int,
        @Header(KafkaHeaders.OFFSET) offset: Long,
        acknowledgment: Acknowledgment
    ) {
        try {
            logger.info("Received account event: ${event.eventType} from topic: $topic, partition: $partition, offset: $offset")
            onAccountEvent(event)
            acknowledgment.acknowledge()
        } catch (e: Exception) {
            logger.error("Error processing account event: ${event.eventType}", e)
            handleError(event, e)
        }
    }
    
    @KafkaListener(topics = ["transaction-events"], groupId = "toss-consumer")
    fun handleTransactionEvent(
        @Payload event: Event,
        @Header(KafkaHeaders.RECEIVED_TOPIC) topic: String,
        @Header(KafkaHeaders.RECEIVED_PARTITION_ID) partition: Int,
        @Header(KafkaHeaders.OFFSET) offset: Long,
        acknowledgment: Acknowledgment
    ) {
        try {
            logger.info("Received transaction event: ${event.eventType} from topic: $topic, partition: $partition, offset: $offset")
            onTransactionEvent(event)
            acknowledgment.acknowledge()
        } catch (e: Exception) {
            logger.error("Error processing transaction event: ${event.eventType}", e)
            handleError(event, e)
        }
    }
    
    @KafkaListener(topics = ["ledger-events"], groupId = "toss-consumer")
    fun handleLedgerEvent(
        @Payload event: Event,
        @Header(KafkaHeaders.RECEIVED_TOPIC) topic: String,
        @Header(KafkaHeaders.RECEIVED_PARTITION_ID) partition: Int,
        @Header(KafkaHeaders.OFFSET) offset: Long,
        acknowledgment: Acknowledgment
    ) {
        try {
            logger.info("Received ledger event: ${event.eventType} from topic: $topic, partition: $partition, offset: $offset")
            onLedgerEvent(event)
            acknowledgment.acknowledge()
        } catch (e: Exception) {
            logger.error("Error processing ledger event: ${event.eventType}", e)
            handleError(event, e)
        }
    }
    
    @KafkaListener(topics = ["notification-events"], groupId = "toss-consumer")
    fun handleNotificationEvent(
        @Payload event: Event,
        @Header(KafkaHeaders.RECEIVED_TOPIC) topic: String,
        @Header(KafkaHeaders.RECEIVED_PARTITION_ID) partition: Int,
        @Header(KafkaHeaders.OFFSET) offset: Long,
        acknowledgment: Acknowledgment
    ) {
        try {
            logger.info("Received notification event: ${event.eventType} from topic: $topic, partition: $partition, offset: $offset")
            onNotificationEvent(event)
            acknowledgment.acknowledge()
        } catch (e: Exception) {
            logger.error("Error processing notification event: ${event.eventType}", e)
            handleError(event, e)
        }
    }
    
    @KafkaListener(topics = ["transfer-events"], groupId = "toss-consumer")
    fun handleTransferEvent(
        @Payload event: Event,
        @Header(KafkaHeaders.RECEIVED_TOPIC) topic: String,
        @Header(KafkaHeaders.RECEIVED_PARTITION_ID) partition: Int,
        @Header(KafkaHeaders.OFFSET) offset: Long,
        acknowledgment: Acknowledgment
    ) {
        try {
            logger.info("Received transfer event: ${event.eventType} from topic: $topic, partition: $partition, offset: $offset")
            onTransferEvent(event)
            acknowledgment.acknowledge()
        } catch (e: Exception) {
            logger.error("Error processing transfer event: ${event.eventType}", e)
            handleError(event, e)
        }
    }
    
    // Abstract methods to be implemented by concrete consumers
    protected open fun onUserEvent(event: Event) {}
    protected open fun onAccountEvent(event: Event) {}
    protected open fun onTransactionEvent(event: Event) {}
    protected open fun onLedgerEvent(event: Event) {}
    protected open fun onNotificationEvent(event: Event) {}
    protected open fun onTransferEvent(event: Event) {}
    
    protected open fun handleError(event: Event, error: Exception) {
        logger.error("Failed to process event: ${event.eventType}", error)
        // Implement error handling strategy (retry, DLQ, etc.)
    }
}
