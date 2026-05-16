package com.example.platform.analytics.domain;

import com.example.platform.common.domain.AbstractAuditableEntity;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Analytics event entity for capturing user behavior and system metrics.
 * Phase 3: Stores events for analytics dashboards and reporting.
 */
@Entity
@Table(name = "analytics_events", indexes = {
        @Index(name = "idx_analytics_events_tenant", columnList = "tenant_id"),
        @Index(name = "idx_analytics_events_user", columnList = "user_id"),
        @Index(name = "idx_analytics_events_type", columnList = "event_type"),
        @Index(name = "idx_analytics_events_timestamp", columnList = "created_at")
})
public class AnalyticsEventEntity extends AbstractAuditableEntity {

    @Id
    @Column(name = "event_id", nullable = false, length = 64)
    private String eventId;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Column(name = "user_id", length = 64)
    private String userId;

    @Column(name = "workspace_id", length = 64)
    private String workspaceId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "event_category", nullable = false, length = 50)
    private String eventCategory;

    @Column(name = "resource_type", length = 50)
    private String resourceType;

    @Column(name = "resource_id", length = 64)
    private String resourceId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "event_data", columnDefinition = "jsonb", nullable = false)
    private JsonNode eventData; // JSON serialized event properties

    @Column(name = "session_id", length = 64)
    private String sessionId;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    protected AnalyticsEventEntity() {
    }

    public AnalyticsEventEntity(
            String eventId,
            String tenantId,
            String userId,
            String workspaceId,
            String eventType,
            String eventCategory,
            String resourceType,
            String resourceId,
            JsonNode eventData,
            String sessionId,
            String userAgent,
            String ipAddress
    ) {
        this.eventId = eventId;
        this.tenantId = tenantId;
        this.userId = userId;
        this.workspaceId = workspaceId;
        this.eventType = eventType;
        this.eventCategory = eventCategory;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.eventData = eventData;
        this.sessionId = sessionId;
        this.userAgent = userAgent;
        this.ipAddress = ipAddress;
    }

    // Getters
    public String getEventId() { return eventId; }
    public String getTenantId() { return tenantId; }
    public String getUserId() { return userId; }
    public String getWorkspaceId() { return workspaceId; }
    public String getEventType() { return eventType; }
    public String getEventCategory() { return eventCategory; }
    public String getResourceType() { return resourceType; }
    public String getResourceId() { return resourceId; }
    public JsonNode getEventData() { return eventData; }
    public String getSessionId() { return sessionId; }
    public String getUserAgent() { return userAgent; }
    public String getIpAddress() { return ipAddress; }
}
