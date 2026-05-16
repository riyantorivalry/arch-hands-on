package com.example.platform.common.infrastructure;

import com.example.platform.common.domain.DomainEvent;
import com.example.platform.common.domain.DomainEventPublisher;
import com.example.platform.common.domain.OutboxEvent;
import com.example.platform.common.domain.OutboxEventRepository;
import com.example.platform.documents.domain.DocumentCreatedEvent;
import com.example.platform.documents.domain.DocumentUpdatedEvent;
import com.example.platform.identityaccess.domain.WorkspaceMemberAddedEvent;
import com.example.platform.messaging.domain.MessagePostedEvent;
import com.example.platform.tasks.domain.TaskAssignedEvent;
import com.example.platform.tasks.domain.TaskCreatedEvent;
import com.example.platform.tasks.domain.TaskStatusChangedEvent;
import com.example.platform.tenantmanagement.domain.TenantCreatedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Background worker for processing outbox events.
 * Phase 2: Publishes stored events to external systems (Kafka, listeners, etc.).
 *
 * Runs periodically to ensure reliable event publishing with retry logic.
 */
@Component
@ConditionalOnProperty(
    name = "platform.event-publishing.mode",
    havingValue = "outbox",
    matchIfMissing = false
)
public class OutboxEventProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(OutboxEventProcessor.class);

    /**
     * Registry mapping event type strings to their concrete classes.
     * Used for type-aware deserialization of events from the outbox.
     */
    private static final Map<String, Class<? extends DomainEvent>> EVENT_TYPE_REGISTRY = new HashMap<>();

    static {
        // Register all domain event types
        EVENT_TYPE_REGISTRY.put("TenantCreatedEvent", TenantCreatedEvent.class);
        EVENT_TYPE_REGISTRY.put("WorkspaceMemberAddedEvent", WorkspaceMemberAddedEvent.class);
        EVENT_TYPE_REGISTRY.put("TaskCreatedEvent", TaskCreatedEvent.class);
        EVENT_TYPE_REGISTRY.put("TaskAssignedEvent", TaskAssignedEvent.class);
        EVENT_TYPE_REGISTRY.put("TaskStatusChangedEvent", TaskStatusChangedEvent.class);
        EVENT_TYPE_REGISTRY.put("DocumentCreatedEvent", DocumentCreatedEvent.class);
        EVENT_TYPE_REGISTRY.put("DocumentUpdatedEvent", DocumentUpdatedEvent.class);
        EVENT_TYPE_REGISTRY.put("MessagePostedEvent", MessagePostedEvent.class);
    }

    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final DomainEventPublisher externalPublisher;

    @Value("${platform.outbox.max-retries:3}")
    private int maxRetries;

    @Value("${platform.outbox.batch-size:50}")
    private int batchSize;

    @Value("${platform.outbox.cleanup-days:30}")
    private int cleanupDays;

    public OutboxEventProcessor(
            OutboxEventRepository outboxRepository,
            ObjectMapper objectMapper,
            DomainEventPublisher externalPublisher
    ) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
        this.externalPublisher = externalPublisher;
    }

    /**
     * Process outbox events every 5 seconds.
     * Publishes events to external systems and marks them as published.
     */
    @Scheduled(fixedDelay = 5000) // 5 seconds
    @Transactional
    public void processOutboxEvents() {
        List<OutboxEvent> events = outboxRepository.findEventsForRetry(maxRetries);

        if (events.isEmpty()) {
            return;
        }

        LOGGER.debug("Processing {} outbox events", events.size());

        int processed = 0;
        int failed = 0;

        for (OutboxEvent outboxEvent : events) {
            try {
                // Deserialize event
                DomainEvent domainEvent = deserializeEvent(outboxEvent);

                // Publish to external system
                externalPublisher.publish(domainEvent);

                // Mark as published
                outboxEvent.markAsPublished();
                outboxRepository.save(outboxEvent);

                processed++;
                LOGGER.debug("Published outbox event: {} ({})",
                           outboxEvent.getEventType(), outboxEvent.getEventId());

            } catch (Exception e) {
                outboxEvent.recordRetry(e.getMessage());
                outboxRepository.save(outboxEvent);

                failed++;
                LOGGER.warn("Failed to publish outbox event: {} (attempt {}/{}) - {}",
                          outboxEvent.getEventId(),
                          outboxEvent.getRetryCount(),
                          maxRetries,
                          e.getMessage());
            }

            // Process in batches to avoid long-running transactions
            if ((processed + failed) >= batchSize) {
                break;
            }
        }

        if (processed > 0 || failed > 0) {
            LOGGER.info("Outbox processing complete: {} published, {} failed, {} remaining",
                       processed, failed, outboxRepository.countUnpublishedEvents());
        }
    }

    /**
     * Clean up old published events daily.
     */
    @Scheduled(cron = "0 0 2 * * ?") // Daily at 2 AM
    @Transactional
    public void cleanupOldEvents() {
        Instant cutoff = Instant.now().minusSeconds(cleanupDays * 24 * 60 * 60L);
        List<OutboxEvent> oldEvents = outboxRepository.findPublishedEventsBefore(cutoff);

        if (!oldEvents.isEmpty()) {
            outboxRepository.deleteAll(oldEvents);
            LOGGER.info("Cleaned up {} old published events", oldEvents.size());
        }
    }

    /**
     * Deserialize an outbox event to its concrete type based on eventType field.
     * Uses type-aware deserialization to ensure the correct concrete event class is instantiated.
     *
     * @param outboxEvent the outbox event entity containing JSON data and event type
     * @return the deserialized domain event
     * @throws Exception if the event type is unknown or deserialization fails
     */
    private DomainEvent deserializeEvent(OutboxEvent outboxEvent) throws Exception {
        String eventType = outboxEvent.getEventType();

        // Look up the concrete event class from the registry
        Class<? extends DomainEvent> eventClass = EVENT_TYPE_REGISTRY.get(eventType);

        if (eventClass == null) {
            String errorMsg = "Unknown event type: " + eventType +
                             ". Known types: " + EVENT_TYPE_REGISTRY.keySet();
            LOGGER.error("Failed to deserialize outbox event {}: {}", outboxEvent.getEventId(), errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }

        try {
            // Deserialize JSON to the concrete event class
            DomainEvent deserializedEvent = objectMapper.convertValue(outboxEvent.getEventData(), eventClass);

            LOGGER.debug("Successfully deserialized outbox event {} as {}",
                        outboxEvent.getEventId(), eventType);

            return deserializedEvent;
        } catch (Exception e) {
            String errorMsg = "Failed to deserialize event data for event type " + eventType +
                             " (eventId: " + outboxEvent.getEventId() + ")";
            LOGGER.error(errorMsg, e);
            throw new IllegalArgumentException(errorMsg, e);
        }
    }
}
