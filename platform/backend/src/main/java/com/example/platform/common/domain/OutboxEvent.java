package com.example.platform.common.domain;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

/**
 * Outbox event entity for reliable event publishing.
 * Phase 2: Stores events in database before publishing to prevent event loss.
 */
@Table("outbox_events")
public class OutboxEvent {

    @Id
    private Long id;

    @Column("event_id")
    private String eventId;

    @Column("event_type")
    private String eventType;

    @Column("aggregate_type")
    private String aggregateType;

    @Column("aggregate_id")
    private String aggregateId;

    @Column("tenant_id")
    private String tenantId;

    @Column("event_data")
    private JsonNode eventData; // JSON serialized event (stored as jsonb in Postgres)

    @Column("created_at")
    private Instant createdAt;

    @Column("published_at")
    private Instant publishedAt;

    @Column("published")
    private boolean published = false;

    @Column("retry_count")
    private int retryCount = 0;

    @Column("last_error")
    private String lastError;

    // Constructors
    public OutboxEvent() {}

    public OutboxEvent(String eventId, String eventType, String aggregateType,
                      String aggregateId, String tenantId, JsonNode eventData) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.tenantId = tenantId;
        this.eventData = eventData;
        this.createdAt = Instant.now();
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getAggregateType() { return aggregateType; }
    public void setAggregateType(String aggregateType) { this.aggregateType = aggregateType; }

    public String getAggregateId() { return aggregateId; }
    public void setAggregateId(String aggregateId) { this.aggregateId = aggregateId; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public JsonNode getEventData() { return eventData; }
    public void setEventData(JsonNode eventData) { this.eventData = eventData; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getPublishedAt() { return publishedAt; }
    public void setPublishedAt(Instant publishedAt) { this.publishedAt = publishedAt; }

    public boolean isPublished() { return published; }
    public void setPublished(boolean published) { this.published = published; }

    public int getRetryCount() { return retryCount; }
    public void setRetryCount(int retryCount) { this.retryCount = retryCount; }

    public String getLastError() { return lastError; }
    public void setLastError(String lastError) { this.lastError = lastError; }

    /**
     * Mark this event as published.
     */
    public void markAsPublished() {
        this.published = true;
        this.publishedAt = Instant.now();
    }

    /**
     * Increment retry count and record error.
     */
    public void recordRetry(String error) {
        this.retryCount++;
        this.lastError = error;
    }
}
