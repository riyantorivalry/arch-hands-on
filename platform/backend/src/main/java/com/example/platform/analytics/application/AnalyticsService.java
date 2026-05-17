package com.example.platform.analytics.application;

import com.example.platform.analytics.domain.AnalyticsEventEntity;
import com.example.platform.analytics.domain.AnalyticsEventDocument;
import com.example.platform.analytics.infrastructure.AnalyticsEventRepository;
import com.example.platform.analytics.infrastructure.AnalyticsEventMongoRepository;
import com.example.platform.common.web.RequestContext;
import com.example.platform.common.web.RequestContexts;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Analytics service for capturing and querying user behavior events.
 * Phase 3: Provides analytics event capture and basic querying capabilities.
 */
@Service
public class AnalyticsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AnalyticsService.class);

    private final AnalyticsEventRepository postgresRepository;
    private final AnalyticsEventMongoRepository mongoRepository;
    private final ObjectMapper objectMapper;

    public AnalyticsService(
            AnalyticsEventRepository postgresRepository,
            AnalyticsEventMongoRepository mongoRepository,
            ObjectMapper objectMapper
    ) {
        this.postgresRepository = postgresRepository;
        this.mongoRepository = mongoRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void captureEvent(String eventType, String eventCategory, String resourceType,
                           String resourceId, Map<String, Object> eventData) {
        try {
            RequestContext context = RequestContexts.current();
            String eventId = "analytics-" + UUID.randomUUID().toString();
            JsonNode serializedData = objectMapper.valueToTree(eventData);

            AnalyticsEventEntity postgresEvent = new AnalyticsEventEntity(
                    eventId,
                    context.tenantId(),
                    context.userId(),
                    context.workspaceId(),
                    eventType,
                    eventCategory,
                    resourceType,
                    resourceId,
                    serializedData,
                    null, // sessionId - not available in current RequestContext
                    null, // userAgent - not available in current RequestContext
                    null  // ipAddress - not available in current RequestContext
            );
            savePostgres(postgresEvent);

            AnalyticsEventDocument mongoEvent = new AnalyticsEventDocument(
                    eventId,
                    context.tenantId(),
                    context.userId(),
                    context.workspaceId(),
                    eventType,
                    eventCategory,
                    resourceType,
                    resourceId,
                    serializedData,
                    null,
                    null,
                    null
            );
            saveMongo(mongoEvent);
            LOGGER.debug("Captured analytics event: {} for tenant: {}", eventType, context.tenantId());

        } catch (Exception e) {
            LOGGER.error("Failed to capture analytics event", e);
        }
    }

    @Transactional(readOnly = true)
    public List<AnalyticsEventView> getPostgresJsonbEventsForTenant(String tenantId, Instant start, Instant end) {
        return postgresRepository.findByTenantIdAndCreatedAtBetweenOrderByCreatedAtDesc(tenantId, start, end).stream()
                .map(this::toView)
                .toList();
    }

    public List<AnalyticsEventView> getMongoEventsForTenant(String tenantId, Instant start, Instant end) {
        return mongoRepository.findByTenantIdAndCreatedAtBetweenOrderByCreatedAtDesc(tenantId, start, end).stream()
                .map(this::toView)
                .toList();
    }

    @Transactional(readOnly = true)
    public long countPostgresJsonbEvents(String tenantId, String eventType, Instant start, Instant end) {
        return postgresRepository.countByTenantIdAndEventTypeAndCreatedAtBetween(tenantId, eventType, start, end);
    }

    public long countMongoEvents(String tenantId, String eventType, Instant start, Instant end) {
        return mongoRepository.countByTenantIdAndEventTypeAndCreatedAtBetween(tenantId, eventType, start, end);
    }

    @Transactional(readOnly = true)
    public List<String> getEventTypesForTenant(String tenantId) {
        return postgresRepository.findDistinctEventTypesByTenantId(tenantId);
    }

    public List<String> getMongoEventTypesForTenant(String tenantId) {
        var docs = mongoRepository.findDistinctEventTypesByTenantId(tenantId);
        return docs.stream().map(d -> d.getEventType()).distinct().sorted().toList();
    }

    private void savePostgres(AnalyticsEventEntity event) {
        try {
            postgresRepository.save(event);
        } catch (Exception exception) {
            LOGGER.warn("Failed to persist analytics event to Postgres JSONB: {}", event.getEventId(), exception);
        }
    }

    private void saveMongo(AnalyticsEventDocument event) {
        try {
            mongoRepository.save(event);
        } catch (Exception exception) {
            LOGGER.warn("Failed to persist analytics event to MongoDB: {}", event.getEventId(), exception);
        }
    }

    private AnalyticsEventView toView(AnalyticsEventEntity event) {
        return new AnalyticsEventView(
                event.getEventId(),
                event.getTenantId(),
                event.getUserId(),
                event.getWorkspaceId(),
                event.getEventType(),
                event.getEventCategory(),
                event.getResourceType(),
                event.getResourceId(),
                event.getEventData(),
                event.getCreatedAt()
        );
    }

    private AnalyticsEventView toView(AnalyticsEventDocument event) {
        return new AnalyticsEventView(
                event.getEventId(),
                event.getTenantId(),
                event.getUserId(),
                event.getWorkspaceId(),
                event.getEventType(),
                event.getEventCategory(),
                event.getResourceType(),
                event.getResourceId(),
                event.getEventData(),
                event.getCreatedAt()
        );
    }

    public record AnalyticsEventView(
            String eventId,
            String tenantId,
            String userId,
            String workspaceId,
            String eventType,
            String eventCategory,
            String resourceType,
            String resourceId,
            JsonNode eventData,
            Instant createdAt
    ) {
    }

    // Convenience methods for common events
    public void trackPageView(String page) {
        captureEvent("page_view", "engagement", "page", page, Map.of("page", page));
    }

    public void trackFeatureUsage(String feature, Map<String, Object> metadata) {
        captureEvent("feature_usage", "engagement", "feature", feature, metadata);
    }

    public void trackSearch(String query, int resultCount) {
        captureEvent("search", "engagement", "search", null,
                Map.of("query", query, "resultCount", resultCount));
    }

    public void trackError(String errorType, String message) {
        captureEvent("error", "technical", "error", errorType,
                Map.of("message", message));
    }
}
