package com.example.platform.common.infrastructure;

import com.example.platform.common.domain.DomainEvent;
import com.example.platform.common.domain.DomainEventPublisher;
import com.example.platform.common.infrastructure.observability.BusinessMetricsCollector;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * In-process event publisher for Phase 1.
 * Uses Spring's ApplicationEventPublisher to notify local listeners immediately.
 *
 * This is intentionally simple and synchronous:
 * - Events are published after transaction commit
 * - Listeners run in the same transaction context
 * - No external message broker dependency
 *
 * Phase 2+ will replace with async event streaming (Kafka/NATS)
 * without changing the interface.
 */
@Component
@ConditionalOnProperty(
        name = "platform.event-publishing.mode",
        havingValue = "in-process",
        matchIfMissing = false
)
public class InProcessDomainEventPublisher implements DomainEventPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(InProcessDomainEventPublisher.class);

    private final ApplicationEventPublisher applicationEventPublisher;
    private final BusinessMetricsCollector metricsCollector;

    public InProcessDomainEventPublisher(
            ApplicationEventPublisher applicationEventPublisher,
            BusinessMetricsCollector metricsCollector
    ) {
        this.applicationEventPublisher = applicationEventPublisher;
        this.metricsCollector = metricsCollector;
    }

    @Override
    public Mono<Void> publish(DomainEvent event) {
        return Mono.fromRunnable(() -> {
            LOGGER.debug("Publishing domain event: type={}, eventId={}, aggregateId={}",
                    event.getEventType(), event.getEventId(), event.getAggregateId());
            Timer.Sample sample = metricsCollector.startEventPublishingTimer();
            try {
                applicationEventPublisher.publishEvent(event);
                metricsCollector.recordEventPublished();
            } catch (RuntimeException e) {
                metricsCollector.recordEventFailed();
                throw e;
            } finally {
                metricsCollector.stopEventPublishingTimer(sample);
            }
        });
    }
}

