package com.example.platform.common.infrastructure;

import com.example.platform.documents.domain.DocumentCreatedEvent;
import com.example.platform.documents.domain.DocumentUpdatedEvent;
import com.example.platform.identityaccess.domain.WorkspaceMemberAddedEvent;
import com.example.platform.messaging.domain.MessagePostedEvent;
import com.example.platform.tasks.domain.TaskAssignedEvent;
import com.example.platform.tasks.domain.TaskCreatedEvent;
import com.example.platform.tasks.domain.TaskStatusChangedEvent;
import com.example.platform.tenantmanagement.domain.TenantCreatedEvent;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Tracks metrics for all domain events.
 * Phase 1b+: In-process counters for all event types
 * Phase 2+: Export to Prometheus or other metrics system
 */
@Component
public class EventMetricsListener {

    private final ConcurrentHashMap<String, AtomicLong> eventCounts = new ConcurrentHashMap<>();
    private final AtomicLong totalEventCount = new AtomicLong(0);

    @EventListener
    public void onTenantCreated(TenantCreatedEvent event) {
        incrementMetric("TenantCreatedEvent");
    }

    @EventListener
    public void onWorkspaceMemberAdded(WorkspaceMemberAddedEvent event) {
        incrementMetric("WorkspaceMemberAddedEvent");
    }

    @EventListener
    public void onMessagePosted(MessagePostedEvent event) {
        incrementMetric("MessagePostedEvent");
    }

    @EventListener
    public void onDocumentCreated(DocumentCreatedEvent event) {
        incrementMetric("DocumentCreatedEvent");
    }

    @EventListener
    public void onDocumentUpdated(DocumentUpdatedEvent event) {
        incrementMetric("DocumentUpdatedEvent");
    }

    @EventListener
    public void onTaskCreated(TaskCreatedEvent event) {
        incrementMetric("TaskCreatedEvent");
    }

    @EventListener
    public void onTaskAssigned(TaskAssignedEvent event) {
        incrementMetric("TaskAssignedEvent");
    }

    @EventListener
    public void onTaskStatusChanged(TaskStatusChangedEvent event) {
        incrementMetric("TaskStatusChangedEvent");
    }

    private void incrementMetric(String eventType) {
        eventCounts.computeIfAbsent(eventType, key -> new AtomicLong(0)).incrementAndGet();
        totalEventCount.incrementAndGet();
    }

    /**
     * Get count of a specific event type.
     * For testing and observability.
     */
    public long getEventCount(String eventType) {
        AtomicLong count = eventCounts.get(eventType);
        return count != null ? count.get() : 0;
    }

    /**
     * Get total event count across all types.
     * For testing and observability.
     */
    public long getTotalEventCount() {
        return totalEventCount.get();
    }

    /**
     * Get all event metrics for observability.
     * Phase 2+: Expose via /metrics endpoint
     */
    public ConcurrentHashMap<String, Long> getAllMetrics() {
        ConcurrentHashMap<String, Long> metrics = new ConcurrentHashMap<>();
        eventCounts.forEach((key, value) -> metrics.put(key, value.get()));
        metrics.put("total", totalEventCount.get());
        return metrics;
    }

    /**
     * Reset metrics (for testing).
     */
    public void reset() {
        eventCounts.clear();
        totalEventCount.set(0);
    }
}

