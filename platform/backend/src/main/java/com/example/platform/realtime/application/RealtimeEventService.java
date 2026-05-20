package com.example.platform.realtime.application;

import com.example.platform.common.domain.DomainEvent;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Service
public class RealtimeEventService {

    private final AtomicLong versionSequence = new AtomicLong();
    private final Object eventLogLock = new Object();
    private final Deque<RealtimeEvent> eventLog = new ArrayDeque<>();
    private final Sinks.Many<RealtimeEvent> realtimeEvents = Sinks.many().multicast().directBestEffort();

    @Value("${platform.realtime.max-events:1000}")
    private int maxEvents;

    public RealtimeEvent append(String tenantId, String workspaceId, DomainEvent event) {
        RealtimeEvent realtimeEvent = new RealtimeEvent(
                versionSequence.incrementAndGet(),
                "realtime.event.v1",
                event.getEventId(),
                event.getEventType(),
                tenantId,
                workspaceId,
                event.getAggregateId(),
                event.getOccurredAt(),
                Instant.now(),
                event
        );

        synchronized (eventLogLock) {
            eventLog.addLast(realtimeEvent);
            while (eventLog.size() > maxEvents) {
                eventLog.removeFirst();
            }
        }

        realtimeEvents.tryEmitNext(realtimeEvent);
        return realtimeEvent;
    }

    public RealtimePollResponse poll(String tenantId, String workspaceId, long since) {
        List<RealtimeEvent> events = eventsSince(tenantId, workspaceId, since);
        long nextCursor = events.isEmpty() ? versionSequence.get() : events.get(events.size() - 1).version();
        return new RealtimePollResponse(nextCursor, events);
    }

    public Flux<ServerSentEvent<RealtimeEvent>> stream(String tenantId, String workspaceId, long since) {
        long normalizedSince = Math.max(0, since);
        return Flux.defer(() -> Flux.fromIterable(eventsSince(tenantId, workspaceId, normalizedSince))
                        .mergeWith(realtimeEvents.asFlux()
                                .filter(event -> event.version() > normalizedSince)
                                .filter(event -> matches(event, tenantId, workspaceId))))
                .map(event -> ServerSentEvent.<RealtimeEvent>builder(event)
                        .id(Long.toString(event.version()))
                        .event("realtime-event")
                        .build());
    }

    private List<RealtimeEvent> eventsSince(String tenantId, String workspaceId, long since) {
        long normalizedSince = Math.max(0, since);
        synchronized (eventLogLock) {
            return eventLog.stream()
                    .filter(event -> event.version() > normalizedSince)
                    .filter(event -> matches(event, tenantId, workspaceId))
                    .toList();
        }
    }

    private boolean matches(RealtimeEvent event, String tenantId, String workspaceId) {
        if (!event.tenantId().equals(tenantId)) {
            return false;
        }
        return event.workspaceId() == null || event.workspaceId().equals(workspaceId);
    }

    public record RealtimeEvent(
            long version,
            String schemaVersion,
            String eventId,
            String eventType,
            String tenantId,
            String workspaceId,
            String aggregateId,
            Instant occurredAt,
            Instant recordedAt,
            Object payload
    ) {
    }

    public record RealtimePollResponse(long nextCursor, List<RealtimeEvent> events) {
    }
}
