package com.example.platform.common.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Repository for outbox events.
 * Phase 2: Provides queries for reliable event publishing.
 */
@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    /**
     * Find unpublished events ordered by creation time.
     * Used by background worker to publish pending events.
     */
    @Query("SELECT e FROM OutboxEvent e WHERE e.published = false ORDER BY e.createdAt ASC")
    List<OutboxEvent> findUnpublishedEvents();

    /**
     * Find unpublished events for a specific tenant.
     */
    @Query("SELECT e FROM OutboxEvent e WHERE e.published = false AND e.tenantId = :tenantId ORDER BY e.createdAt ASC")
    List<OutboxEvent> findUnpublishedEventsByTenant(@Param("tenantId") String tenantId);

    /**
     * Find events that have failed publishing and need retry.
     * Events with retry_count < maxRetries and not published.
     */
    @Query("SELECT e FROM OutboxEvent e WHERE e.published = false AND e.retryCount < :maxRetries ORDER BY e.createdAt ASC")
    List<OutboxEvent> findEventsForRetry(@Param("maxRetries") int maxRetries);

    /**
     * Count unpublished events (for monitoring).
     */
    @Query("SELECT COUNT(e) FROM OutboxEvent e WHERE e.published = false")
    long countUnpublishedEvents();

    /**
     * Find old published events for cleanup.
     */
    @Query("SELECT e FROM OutboxEvent e WHERE e.published = true AND e.publishedAt < :before ORDER BY e.publishedAt ASC")
    List<OutboxEvent> findPublishedEventsBefore(@Param("before") Instant before);

    /**
     * Check if event with given ID already exists.
     */
    boolean existsByEventId(String eventId);
}
