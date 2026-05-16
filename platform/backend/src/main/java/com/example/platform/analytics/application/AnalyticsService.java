package com.example.platform.analytics.application;

import com.example.platform.analytics.domain.AnalyticsEventEntity;
import com.example.platform.analytics.infrastructure.AnalyticsEventRepository;
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

    private final AnalyticsEventRepository repository;
    private final ObjectMapper objectMapper;

    public AnalyticsService(AnalyticsEventRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void captureEvent(String eventType, String eventCategory, String resourceType,
                           String resourceId, Map<String, Object> eventData) {
        try {
            RequestContext context = RequestContexts.current();
            String eventId = "analytics-" + UUID.randomUUID().toString();
            JsonNode serializedData = objectMapper.valueToTree(eventData);

            AnalyticsEventEntity event = new AnalyticsEventEntity(
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

            repository.save(event);
            LOGGER.debug("Captured analytics event: {} for tenant: {}", eventType, context.tenantId());

        } catch (Exception e) {
            LOGGER.error("Failed to capture analytics event", e);
        }
    }

    public List<AnalyticsEventEntity> getEventsForTenant(String tenantId, Instant start, Instant end) {
        return repository.findByTenantIdAndCreatedAtBetweenOrderByCreatedAtDesc(tenantId, start, end);
    }

    public long countEvents(String tenantId, String eventType, Instant start, Instant end) {
        return repository.countByTenantIdAndEventTypeAndCreatedAtBetween(tenantId, eventType, start, end);
    }

    public List<String> getEventTypesForTenant(String tenantId) {
        return repository.findDistinctEventTypesByTenantId(tenantId);
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
