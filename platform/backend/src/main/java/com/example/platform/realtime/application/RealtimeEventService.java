package com.example.platform.realtime.application;

import com.example.platform.common.domain.DomainEvent;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class RealtimeEventService {

    private final AtomicLong versionSequence = new AtomicLong();
    private final Object eventLogLock = new Object();
    private final Deque<RealtimeEvent> eventLog = new ArrayDeque<>();
    private final Set<SseSubscription> sseSubscriptions = ConcurrentHashMap.newKeySet();

    @Value("${platform.realtime.max-events:1000}")
    private int maxEvents;

    @Value("${platform.realtime.sse-timeout-ms:300000}")
    private long sseTimeoutMs;

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

        publishToSseSubscribers(realtimeEvent);
        return realtimeEvent;
    }

    public RealtimePollResponse poll(String tenantId, String workspaceId, long since) {
        List<RealtimeEvent> events = eventsSince(tenantId, workspaceId, since);
        long nextCursor = events.isEmpty() ? versionSequence.get() : events.get(events.size() - 1).version();
        return new RealtimePollResponse(nextCursor, events);
    }

    public SseEmitter stream(String tenantId, String workspaceId, long since) {
        SseEmitter emitter = new SseEmitter(sseTimeoutMs);
        SseSubscription subscription = new SseSubscription(tenantId, workspaceId, emitter);
        sseSubscriptions.add(subscription);

        emitter.onCompletion(() -> sseSubscriptions.remove(subscription));
        emitter.onTimeout(() -> {
            sseSubscriptions.remove(subscription);
            emitter.complete();
        });
        emitter.onError(error -> sseSubscriptions.remove(subscription));

        for (RealtimeEvent event : eventsSince(tenantId, workspaceId, since)) {
            if (!send(emitter, event)) {
                sseSubscriptions.remove(subscription);
                break;
            }
        }

        return emitter;
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

    private void publishToSseSubscribers(RealtimeEvent event) {
        List<SseSubscription> failedSubscriptions = new ArrayList<>();
        for (SseSubscription subscription : sseSubscriptions) {
            if (matches(event, subscription.tenantId(), subscription.workspaceId())
                    && !send(subscription.emitter(), event)) {
                failedSubscriptions.add(subscription);
            }
        }
        sseSubscriptions.removeAll(failedSubscriptions);
    }

    private boolean matches(RealtimeEvent event, String tenantId, String workspaceId) {
        if (!event.tenantId().equals(tenantId)) {
            return false;
        }
        return event.workspaceId() == null || event.workspaceId().equals(workspaceId);
    }

    private boolean send(SseEmitter emitter, RealtimeEvent event) {
        try {
            synchronized (emitter) {
                emitter.send(SseEmitter.event()
                        .id(Long.toString(event.version()))
                        .name("realtime-event")
                        .data(event));
            }
            return true;
        } catch (IOException | IllegalStateException exception) {
            emitter.completeWithError(exception);
            return false;
        }
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

    private record SseSubscription(String tenantId, String workspaceId, SseEmitter emitter) {
    }
}
