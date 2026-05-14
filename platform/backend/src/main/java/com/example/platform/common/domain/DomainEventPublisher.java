package com.example.platform.common.domain;

import java.util.List;

/**
 * Platform-wide event bus for publishing domain events.
 * In Phase 1, this is in-process only.
 * Phase 2 will extract to Kafka/NATS while keeping this interface unchanged.
 */
public interface DomainEventPublisher {

    /**
     * Publish a single domain event
     */
    void publish(DomainEvent event);

    /**
     * Publish multiple domain events (batched as a transaction commit)
     */
    void publishAll(List<DomainEvent> events);
}

