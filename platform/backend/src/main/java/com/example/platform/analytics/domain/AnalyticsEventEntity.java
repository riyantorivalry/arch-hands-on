package com.example.platform.analytics.domain;

import com.example.platform.common.domain.AbstractAuditableEntity;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Analytics event entity for capturing user behavior and system metrics.
 * Phase 3: Stores events for analytics dashboards and reporting.
 */
@Table("analytics_events")
public class AnalyticsEventEntity extends AbstractAuditableEntity {

    @Id
    @Column("event_id")
    private String eventId;

    @Column("tenant_id")
    private String tenantId;

    @Column("user_id")
    private String userId;

    @Column("workspace_id")
    private String workspaceId;

    @Column("event_type")
    private String eventType;

    @Column("event_category")
    private String eventCategory;

    @Column("resource_type")
    private String resourceType;

    @Column("resource_id")
    private String resourceId;

    @Column("event_data")
    private JsonNode eventData; // JSON serialized event properties

    @Column("session_id")
    private String sessionId;

    @Column("user_agent")
    private String userAgent;

    @Column("ip_address")
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

    @Override
    public Object getId() {
        return eventId;
    }
}
