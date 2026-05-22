package com.example.platform.common.domain;

import java.time.Instant;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Repository for outbox events.
 * Phase 2: Provides queries for reliable event publishing.
 */
@Repository
public interface OutboxEventRepository extends ReactiveCrudRepository<OutboxEvent, Long> {

    /**
     * Find unpublished events ordered by creation time.
     * Used by background worker to publish pending events.
     */
    @Query("select * from outbox_events where published = false order by created_at asc")
    Flux<OutboxEvent> findUnpublishedEvents();

    /**
     * Find unpublished events for a specific tenant.
     */
    @Query("select * from outbox_events where published = false and tenant_id = :tenantId order by created_at asc")
    Flux<OutboxEvent> findUnpublishedEventsByTenant(@Param("tenantId") String tenantId);

    /**
     * Find events that have failed publishing and need retry.
     * Events with retry_count < maxRetries and not published.
     */
    @Query("select * from outbox_events where published = false and retry_count < :maxRetries order by created_at asc")
    Flux<OutboxEvent> findEventsForRetry(@Param("maxRetries") int maxRetries);

    /**
     * Count unpublished events (for monitoring).
     */
    @Query("select count(*) from outbox_events where published = false")
    Mono<Long> countUnpublishedEvents();

    /**
     * Find old published events for cleanup.
     */
    @Query("select * from outbox_events where published = true and published_at < :before order by published_at asc")
    Flux<OutboxEvent> findPublishedEventsBefore(@Param("before") Instant before);

    /**
     * Check if event with given ID already exists.
     */
    Mono<Boolean> existsByEventId(String eventId);

    @Modifying
    @Query("""
            insert into outbox_events (
                event_id,
                event_type,
                aggregate_type,
                aggregate_id,
                tenant_id,
                event_data,
                created_at,
                published,
                retry_count
            )
            values (
                :eventId,
                :eventType,
                :aggregateType,
                :aggregateId,
                :tenantId,
                cast(:eventData as jsonb),
                :createdAt,
                :published,
                :retryCount
            )
            """)
    Mono<Integer> insertEvent(
            @Param("eventId") String eventId,
            @Param("eventType") String eventType,
            @Param("aggregateType") String aggregateType,
            @Param("aggregateId") String aggregateId,
            @Param("tenantId") String tenantId,
            @Param("eventData") String eventData,
            @Param("createdAt") Instant createdAt,
            @Param("published") boolean published,
            @Param("retryCount") int retryCount
    );

    @Modifying
    @Query("""
            update outbox_events
            set published = true,
                published_at = :publishedAt
            where event_id = :eventId
            """)
    Mono<Integer> markPublished(
            @Param("eventId") String eventId,
            @Param("publishedAt") Instant publishedAt
    );

    @Modifying
    @Query("""
            update outbox_events
            set retry_count = :retryCount,
                last_error = :lastError
            where event_id = :eventId
            """)
    Mono<Integer> recordFailure(
            @Param("eventId") String eventId,
            @Param("retryCount") int retryCount,
            @Param("lastError") String lastError
    );
}
