package com.example.platform.common.infrastructure;

import com.example.platform.common.domain.DomainEvent;
import com.example.platform.common.domain.DomainEventPublisher;
import com.example.platform.common.domain.OutboxEvent;
import com.example.platform.common.domain.OutboxEventRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Outbox pattern implementation for reliable event publishing.
 * Phase 2: Stores events in database first, then publishes asynchronously.
 *
 * This ensures events are never lost even if the application crashes
 * after database commit but before external publishing.
 */
@Component
@ConditionalOnProperty(
    name = "platform.event-publishing.mode",
    havingValue = "outbox",
    matchIfMissing = false
)
public class OutboxDomainEventPublisher implements DomainEventPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(OutboxDomainEventPublisher.class);

    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public OutboxDomainEventPublisher(
            OutboxEventRepository outboxRepository,
            ObjectMapper objectMapper
    ) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public void publish(DomainEvent event) {
        try {
            // Check if event already exists (idempotency)
            if (outboxRepository.existsByEventId(event.getEventId())) {
                LOGGER.debug("Event already exists in outbox, skipping: {}", event.getEventId());
                return;
            }

            // Serialize event to JSON
            JsonNode eventData = objectMapper.valueToTree(event);

            // Determine aggregate type from event class
            String aggregateType = determineAggregateType(event);

            // Create outbox event
            OutboxEvent outboxEvent = new OutboxEvent(
                event.getEventId(),
                event.getEventType(),
                aggregateType,
                event.getAggregateId(),
                event.getTenantId(),
                eventData
            );

            // Save to database (within transaction)
            outboxRepository.save(outboxEvent);

            LOGGER.info("Event stored in outbox: type={} id={} aggregate={}",
                       event.getEventType(), event.getEventId(), event.getAggregateId());

        } catch (Exception e) {
            LOGGER.error("Failed to store event in outbox: {}", event.getEventType(), e);
            throw new RuntimeException("Failed to store event in outbox", e);
        }
    }

    @Override
    @Transactional
    public void publishAll(java.util.List<DomainEvent> events) {
        events.forEach(this::publish);
    }

    private String determineAggregateType(DomainEvent event) {
        String className = event.getClass().getSimpleName();
        if (className.contains("Tenant")) {
            return "Tenant";
        } else if (className.contains("Workspace")) {
            return "Workspace";
        } else if (className.contains("User") || className.contains("Member")) {
            return "User";
        } else if (className.contains("Message")) {
            return "Message";
        } else if (className.contains("Channel")) {
            return "Channel";
        } else if (className.contains("Document")) {
            return "Document";
        } else if (className.contains("Task")) {
            return "Task";
        }
        return "Unknown";
    }
}
