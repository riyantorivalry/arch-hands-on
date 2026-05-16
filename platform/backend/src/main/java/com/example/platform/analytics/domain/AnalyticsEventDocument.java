package com.example.platform.analytics.domain;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * MongoDB document representation of analytics events. Used only by AnalyticsService to persist events into MongoDB.
 */
@Document(collection = "analytics_events")
public class AnalyticsEventDocument {

    @Id
    private String eventId;

    @Field("tenant_id")
    private String tenantId;

    @Field("user_id")
    private String userId;

    @Field("workspace_id")
    private String workspaceId;

    @Field("event_type")
    private String eventType;

    @Field("event_category")
    private String eventCategory;

    @Field("resource_type")
    private String resourceType;

    @Field("resource_id")
    private String resourceId;

    @Field("event_data")
    private JsonNode eventData;

    @Field("session_id")
    private String sessionId;

    @Field("user_agent")
    private String userAgent;

    @Field("ip_address")
    private String ipAddress;

    @Field("created_at")
    private Instant createdAt = Instant.now();

    protected AnalyticsEventDocument() {
    }

    public AnalyticsEventDocument(String eventId, String tenantId, String userId, String workspaceId,
                                  String eventType, String eventCategory, String resourceType, String resourceId,
                                  JsonNode eventData, String sessionId, String userAgent, String ipAddress) {
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
        this.createdAt = Instant.now();
    }

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
    public Instant getCreatedAt() { return createdAt; }
}

