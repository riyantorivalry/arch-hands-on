package com.example.platform.common.infrastructure.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Tags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Collects and records business-level metrics.
 * Tracks domain operations, workspaces, tasks, documents, etc.
 */
public class BusinessMetricsCollector {
    private static final Logger logger = LoggerFactory.getLogger(BusinessMetricsCollector.class);

    private final MeterRegistry meterRegistry;

    // Counters
    private final Counter taskCreatedCounter;
    private final Counter taskCompletedCounter;
    private final Counter taskFailedCounter;
    private final Counter documentUploadedCounter;
    private final Counter documentAccessCounter;
    private final Counter sessionCreatedCounter;
    private final Counter collaborationEventCounter;
    private final Counter eventPublishedCounter;
    private final Counter eventFailedCounter;
    private final Counter apiErrorCounter;

    // Timers
    private final Timer taskProcessingTimer;
    private final Timer documentProcessingTimer;
    private final Timer collaborationLatencyTimer;
    private final Timer eventPublishingTimer;
    private final Timer databaseQueryTimer;
    private final Timer apiResponseTimer;

    public BusinessMetricsCollector(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        Tags businessTags = Tags.of("service", "platform-backend", "component", "business");

        // Initialize Counters
        this.taskCreatedCounter = Counter.builder("tasks.created")
                .description("Total tasks created")
                .tags(businessTags)
                .register(meterRegistry);

        this.taskCompletedCounter = Counter.builder("tasks.completed")
                .description("Total tasks completed successfully")
                .tags(businessTags)
                .register(meterRegistry);

        this.taskFailedCounter = Counter.builder("tasks.failed")
                .description("Total tasks failed")
                .tags(businessTags)
                .register(meterRegistry);

        this.documentUploadedCounter = Counter.builder("documents.uploaded")
                .description("Total documents uploaded")
                .tags(businessTags)
                .register(meterRegistry);

        this.documentAccessCounter = Counter.builder("documents.accessed")
                .description("Total document access events")
                .tags(businessTags)
                .register(meterRegistry);

        this.sessionCreatedCounter = Counter.builder("sessions.created")
                .description("Total collaboration sessions created")
                .tags(businessTags)
                .register(meterRegistry);

        this.collaborationEventCounter = Counter.builder("collaboration.events")
                .description("Total collaboration events recorded")
                .tags(businessTags)
                .register(meterRegistry);

        this.eventPublishedCounter = Counter.builder("events.published")
                .description("Total domain events published")
                .tags(businessTags)
                .register(meterRegistry);

        this.eventFailedCounter = Counter.builder("events.failed")
                .description("Total domain events failed to publish")
                .tags(businessTags)
                .register(meterRegistry);

        this.apiErrorCounter = Counter.builder("api.errors")
                .description("Total API errors")
                .tags(businessTags)
                .register(meterRegistry);

        // Initialize Timers
        this.taskProcessingTimer = Timer.builder("task.processing.time")
                .description("Time taken to process a task")
                .tags(businessTags)
                .register(meterRegistry);

        this.documentProcessingTimer = Timer.builder("document.processing.time")
                .description("Time taken to process a document")
                .tags(businessTags)
                .register(meterRegistry);

        this.collaborationLatencyTimer = Timer.builder("collaboration.latency")
                .description("Latency of collaboration operations")
                .tags(businessTags)
                .register(meterRegistry);

        this.eventPublishingTimer = Timer.builder("event.publishing.time")
                .description("Time taken to publish an event")
                .tags(businessTags)
                .register(meterRegistry);

        this.databaseQueryTimer = Timer.builder("database.query.time")
                .description("Time taken for database queries")
                .tags(businessTags)
                .register(meterRegistry);

        this.apiResponseTimer = Timer.builder("api.response.time")
                .description("API endpoint response time")
                .tags(businessTags)
                .register(meterRegistry);
    }

    // Task Metrics

    public void recordTaskCreated() {
        taskCreatedCounter.increment();
        logger.info("Task created - total: {}", taskCreatedCounter.count());
    }

    public void recordTaskCompleted() {
        taskCompletedCounter.increment();
        logger.info("Task completed - total: {}", taskCompletedCounter.count());
    }

    public void recordTaskFailed() {
        taskFailedCounter.increment();
        logger.warn("Task failed - total: {}", taskFailedCounter.count());
    }

    public Timer.Sample startTaskProcessingTimer() {
        return Timer.start(meterRegistry);
    }

    public void stopTaskProcessingTimer(Timer.Sample sample) {
        sample.stop(taskProcessingTimer);
    }

    // Document Metrics

    public void recordDocumentUploaded() {
        documentUploadedCounter.increment();
        logger.info("Document uploaded - total: {}", documentUploadedCounter.count());
    }

    public void recordDocumentAccessed() {
        documentAccessCounter.increment();
    }

    public Timer.Sample startDocumentProcessingTimer() {
        return Timer.start(meterRegistry);
    }

    public void stopDocumentProcessingTimer(Timer.Sample sample) {
        sample.stop(documentProcessingTimer);
    }

    // Session Metrics

    public void recordSessionCreated() {
        sessionCreatedCounter.increment();
        logger.info("Collaboration session created - total: {}", sessionCreatedCounter.count());
    }

    // Collaboration Metrics

    public void recordCollaborationEvent() {
        collaborationEventCounter.increment();
    }

    public Timer.Sample startCollaborationLatencyTimer() {
        return Timer.start(meterRegistry);
    }

    public void stopCollaborationLatencyTimer(Timer.Sample sample) {
        sample.stop(collaborationLatencyTimer);
    }

    // Event Publishing Metrics

    public void recordEventPublished() {
        eventPublishedCounter.increment();
        logger.debug("Event published - total: {}", eventPublishedCounter.count());
    }

    public void recordEventFailed() {
        eventFailedCounter.increment();
        logger.warn("Event publishing failed - total: {}", eventFailedCounter.count());
    }

    public Timer.Sample startEventPublishingTimer() {
        return Timer.start(meterRegistry);
    }

    public void stopEventPublishingTimer(Timer.Sample sample) {
        sample.stop(eventPublishingTimer);
    }

    // API Metrics

    public void recordApiError() {
        apiErrorCounter.increment();
    }

    public Timer.Sample startApiResponseTimer() {
        return Timer.start(meterRegistry);
    }

    public void stopApiResponseTimer(Timer.Sample sample) {
        sample.stop(apiResponseTimer);
    }

    // Database Metrics

    public Timer.Sample startDatabaseQueryTimer() {
        return Timer.start(meterRegistry);
    }

    public void stopDatabaseQueryTimer(Timer.Sample sample) {
        sample.stop(databaseQueryTimer);
    }

    // Gauge/Status Metrics

    public void recordGaugeMetric(String name, double value) {
        meterRegistry.gauge("business." + name, value);
    }

    public void recordGaugeMetricWithTags(String name, double value, String... tagKeyValues) {
        meterRegistry.gauge("business." + name, Tags.of(tagKeyValues), value);
    }
}

