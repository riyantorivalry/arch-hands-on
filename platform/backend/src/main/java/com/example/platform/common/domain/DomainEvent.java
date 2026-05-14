package com.example.platform.common.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Base class for all domain events.
 * Events are published after a successful transaction and can be consumed by other modules.
 * Phase 1 uses in-process publishing; Phase 2+ will extract to event streams.
 */
public abstract class DomainEvent {
    private final String eventId;
    private final Instant occurredAt;
    private final String tenantId;
    private final String aggregateId;

    protected DomainEvent(String tenantId, String aggregateId) {
        this.eventId = UUID.randomUUID().toString();
        this.occurredAt = Instant.now();
        this.tenantId = tenantId;
        this.aggregateId = aggregateId;
    }

    public String getEventId() {
        return eventId;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public abstract String getEventType();
}

