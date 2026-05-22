package com.example.platform.analytics.application;

import com.example.platform.analytics.domain.AnalyticsEventEntity;
import com.example.platform.analytics.domain.AnalyticsEventDocument;
import com.example.platform.analytics.infrastructure.AnalyticsEventRepository;
import com.example.platform.analytics.infrastructure.AnalyticsEventMongoRepository;
import com.example.platform.common.web.RequestContext;
import com.example.platform.common.web.RequestContextHolder;
import com.example.platform.common.web.RequestContexts;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import reactor.core.publisher.Mono;

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

    public Mono<Void> captureEvent(String eventType, String eventCategory, String resourceType,
                           String resourceId, Map<String, Object> eventData) {
        return Mono.deferContextual(contextView -> {
            try {
                RequestContext context = RequestContextHolder.get(contextView).orElseGet(RequestContexts::current);
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
            Mono<Void> postgresSave = savePostgres(postgresEvent);

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
            Mono<Void> mongoSave = saveMongo(mongoEvent);
            LOGGER.debug("Captured analytics event: {} for tenant: {}", eventType, context.tenantId());

                return Mono.whenDelayError(postgresSave, mongoSave);
            } catch (Exception e) {
                LOGGER.error("Failed to capture analytics event", e);
                return Mono.empty();
            }
        });
    }

    public Mono<List<AnalyticsEventView>> getPostgresJsonbEventsForTenant(String tenantId, Instant start, Instant end) {
        return postgresRepository.findByTenantIdAndCreatedAtBetweenOrderByCreatedAtDesc(tenantId, start, end)
                .map(this::toView)
                .collectList();
    }

    public Mono<List<AnalyticsEventView>> getMongoEventsForTenant(String tenantId, Instant start, Instant end) {
        return mongoRepository.findByTenantIdAndCreatedAtBetweenOrderByCreatedAtDesc(tenantId, start, end)
                .map(this::toView)
                .collectList();
    }

    public Mono<Long> countPostgresJsonbEvents(String tenantId, String eventType, Instant start, Instant end) {
        return postgresRepository.countByTenantIdAndEventTypeAndCreatedAtBetween(tenantId, eventType, start, end);
    }

    public Mono<Long> countMongoEvents(String tenantId, String eventType, Instant start, Instant end) {
        return mongoRepository.countByTenantIdAndEventTypeAndCreatedAtBetween(tenantId, eventType, start, end);
    }

    public Mono<List<String>> getEventTypesForTenant(String tenantId) {
        return postgresRepository.findDistinctEventTypesByTenantId(tenantId).collectList();
    }

    public Mono<List<String>> getMongoEventTypesForTenant(String tenantId) {
        return mongoRepository.findDistinctEventTypesByTenantId(tenantId)
                .map(AnalyticsEventDocument::getEventType)
                .distinct()
                .sort()
                .collectList();
    }

    private Mono<Void> savePostgres(AnalyticsEventEntity event) {
        return postgresRepository.save(event)
                .then()
                .onErrorResume(exception -> {
                    LOGGER.warn("Failed to persist analytics event to Postgres JSONB: {}", event.getEventId(), exception);
                    return Mono.empty();
                });
    }

    private Mono<Void> saveMongo(AnalyticsEventDocument event) {
        return mongoRepository.save(event)
                .then()
                .onErrorResume(exception -> {
                    LOGGER.warn("Failed to persist analytics event to MongoDB: {}", event.getEventId(), exception);
                    return Mono.empty();
                });
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
    public Mono<Void> trackPageView(String page) {
        return captureEvent("page_view", "engagement", "page", page, Map.of("page", page));
    }

    public Mono<Void> trackFeatureUsage(String feature, Map<String, Object> metadata) {
        return captureEvent("feature_usage", "engagement", "feature", feature, metadata);
    }

    public Mono<Void> trackSearch(String query, int resultCount) {
        return captureEvent("search", "engagement", "search", null,
                Map.of("query", query, "resultCount", resultCount));
    }

    public Mono<Void> trackError(String errorType, String message) {
        return captureEvent("error", "technical", "error", errorType,
                Map.of("message", message));
    }
}
