package com.example.platform.common.infrastructure;

import com.example.platform.common.domain.DomainEvent;
import com.example.platform.common.domain.DomainEventPublisher;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

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
public class InProcessDomainEventPublisher implements DomainEventPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(InProcessDomainEventPublisher.class);

    private final ApplicationEventPublisher applicationEventPublisher;

    public InProcessDomainEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public void publish(DomainEvent event) {
        LOGGER.debug("Publishing domain event: type={}, eventId={}, aggregateId={}",
                event.getEventType(), event.getEventId(), event.getAggregateId());
        applicationEventPublisher.publishEvent(event);
    }

    @Override
    public void publishAll(List<DomainEvent> events) {
        LOGGER.debug("Publishing {} domain events", events.size());
        for (DomainEvent event : events) {
            publish(event);
        }
    }
}

