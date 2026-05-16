package com.example.platform.common.infrastructure;

import com.example.platform.common.domain.DomainEvent;
import com.example.platform.common.domain.DomainEventPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

/**
 * Publishes domain events to Kafka topics for distributed async processing.
 * Phase 2: Replaces InProcessDomainEventPublisher
 *
 * Topics created:
 * - domain.events (all events)
 * - tenant.events
 * - identity.events
 * - messaging.events
 * - documents.events
 * - tasks.events
 */
@Component
@ConditionalOnProperty(
    name = "platform.event-publishing.mode",
    havingValue = "kafka",
    matchIfMissing = false
)
public class KafkaDomainEventPublisher implements DomainEventPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaDomainEventPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${platform.event-publishing.kafka.enabled:false}")
    private boolean kafkaEnabled;

    public KafkaDomainEventPublisher(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publish(DomainEvent event) {
        if (!kafkaEnabled) {
            LOGGER.debug("Kafka event publishing disabled, skipping: {}", event.getEventType());
            return;
        }

        try {
            String eventType = event.getEventType();
            String tenantId = event.getTenantId();
            String eventJson = objectMapper.writeValueAsString(event);

            // Determine topic based on event type
            String topic = getTopicForEvent(eventType);

            // Create message with headers for filtering and tracing
            Message<String> message = MessageBuilder
                    .withPayload(eventJson)
                    .setHeader(KafkaHeaders.TOPIC, topic)
                    .setHeader("eventType", eventType)
                    .setHeader("eventId", event.getEventId())
                    .setHeader("tenantId", tenantId)
                    .setHeader("occurredAt", event.getOccurredAt().toString())
                    .build();

            // Send asynchronously with callback
            kafkaTemplate.send(message)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            LOGGER.error("Failed to publish event to Kafka: {} on topic {}", eventType, topic, ex);
                        } else {
                            LOGGER.info(
                                    "Event published: type={} id={} tenant={} partition={} offset={}",
                                    eventType,
                                    event.getEventId(),
                                    tenantId,
                                    result.getRecordMetadata().partition(),
                                    result.getRecordMetadata().offset()
                            );
                        }
                    });

        } catch (Exception e) {
            LOGGER.error("Error serializing domain event: {}", event.getEventType(), e);
            throw new RuntimeException("Failed to publish event", e);
        }
    }

    @Override
    public void publishAll(java.util.List<DomainEvent> events) {
        events.forEach(this::publish);
    }

    private String getTopicForEvent(String eventType) {
        if (eventType.contains("Tenant")) {
            return "tenant.events";
        } else if (eventType.contains("Member") || eventType.contains("Workspace")) {
            return "identity.events";
        } else if (eventType.contains("Message")) {
            return "messaging.events";
        } else if (eventType.contains("Document")) {
            return "documents.events";
        } else if (eventType.contains("Task")) {
            return "tasks.events";
        }
        return "domain.events"; // Default topic
    }
}

