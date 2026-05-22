package com.example.platform.common.infrastructure;

import com.example.platform.common.domain.DomainEvent;
import com.example.platform.common.domain.DomainEventPublisher;
import com.example.platform.common.domain.OutboxEvent;
import com.example.platform.common.domain.OutboxEventRepository;
import com.example.platform.common.infrastructure.observability.BusinessMetricsCollector;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

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
    private final BusinessMetricsCollector metricsCollector;

    public OutboxDomainEventPublisher(
            OutboxEventRepository outboxRepository,
            ObjectMapper objectMapper,
            BusinessMetricsCollector metricsCollector
    ) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
        this.metricsCollector = metricsCollector;
    }

    @Override
    @Transactional(transactionManager = "connectionFactoryTransactionManager")
    public Mono<Void> publish(DomainEvent event) {
        return Mono.defer(() -> {
            Timer.Sample sample = metricsCollector.startEventPublishingTimer();
            JsonNode eventData = objectMapper.valueToTree(event);
            String aggregateType = determineAggregateType(event);
            OutboxEvent outboxEvent = new OutboxEvent(
                    event.getEventId(),
                    event.getEventType(),
                    aggregateType,
                    event.getAggregateId(),
                    event.getTenantId(),
                    eventData
            );

            return outboxRepository.existsByEventId(event.getEventId())
                    .flatMap(exists -> {
                        if (exists) {
                            LOGGER.debug("Event already exists in outbox, skipping: {}", event.getEventId());
                            return Mono.empty();
                        }
                        return outboxRepository.insertEvent(
                                        outboxEvent.getEventId(),
                                        outboxEvent.getEventType(),
                                        outboxEvent.getAggregateType(),
                                        outboxEvent.getAggregateId(),
                                        outboxEvent.getTenantId(),
                                        outboxEvent.getEventData().toString(),
                                        outboxEvent.getCreatedAt(),
                                        outboxEvent.isPublished(),
                                        outboxEvent.getRetryCount()
                                )
                                .doOnSuccess(inserted -> {
                                    metricsCollector.recordEventPublished();
                                    LOGGER.info("Event stored in outbox: type={} id={} aggregate={}",
                                            event.getEventType(), event.getEventId(), event.getAggregateId());
                                })
                                .then();
                    })
                    .doOnError(e -> {
                        metricsCollector.recordEventFailed();
                        LOGGER.error("Failed to store event in outbox: {}", event.getEventType(), e);
                    })
                    .onErrorMap(e -> new RuntimeException("Failed to store event in outbox", e))
                    .doFinally(signalType -> metricsCollector.stopEventPublishingTimer(sample));
        });
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
