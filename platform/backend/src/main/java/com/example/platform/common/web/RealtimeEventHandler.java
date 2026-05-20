package com.example.platform.common.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.platform.realtime.application.RealtimeEventService.RealtimeEvent;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

/**
 * WebSocket handler for realtime event broadcasts.
 * Manages active WebSocket connections and broadcasts events to subscribed clients.
 *
 * Each session can subscribe to events for their workspace/tenant.
 * Events are broadcast to connected clients in real-time via WebSocket.
 */
@Component
public class RealtimeEventHandler implements WebSocketHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(RealtimeEventHandler.class);

    private final ObjectMapper objectMapper;
    // Map of sessionId -> outbound session state
    private final ConcurrentHashMap<String, SessionState> sessions = new ConcurrentHashMap<>();
    // Map of sessionId -> Set of subscribed workspaceIds
    private final ConcurrentHashMap<String, Set<String>> subscriptions = new ConcurrentHashMap<>();

    public RealtimeEventHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        String sessionId = session.getId();
        SessionState state = new SessionState(Sinks.many().multicast().directBestEffort());
        sessions.put(sessionId, state);
        subscriptions.put(sessionId, Collections.synchronizedSet(new HashSet<>()));

        LOGGER.info("WebSocket connection established: {}", sessionId);

        Mono<Void> inbound = session.receive()
                .map(WebSocketMessage::getPayloadAsText)
                .doOnNext(content -> handleTextMessage(sessionId, state, content))
                .doOnError(exception -> LOGGER.error("WebSocket transport error for session {}", sessionId, exception))
                .doFinally(signalType -> state.outboundMessages().tryEmitComplete())
                .then();

        Mono<Void> outbound = session.send(state.outboundMessages().asFlux()
                .map(this::serialize)
                .map(session::textMessage));

        return Mono.when(inbound, outbound)
                .doFinally(signalType -> {
                    sessions.remove(sessionId);
                    subscriptions.remove(sessionId);
                    LOGGER.info("WebSocket connection closed: {}", sessionId);
                });
    }

    private void handleTextMessage(String sessionId, SessionState state, String content) {
        try {
            // Parse subscription messages like {"action": "subscribe", "workspaceId": "workspace-123"}
            RealtimeMessage message = objectMapper.readValue(content, RealtimeMessage.class);

            if ("subscribe".equals(message.action)) {
                subscriptions.get(sessionId).add(message.workspaceId);
                LOGGER.debug("Session {} subscribed to workspace: {}", sessionId, message.workspaceId);

                state.outboundMessages().tryEmitNext(new RealtimeMessage(
                    "subscribed",
                    message.workspaceId,
                    null,
                    "Successfully subscribed to workspace"
                ));
            } else if ("unsubscribe".equals(message.action)) {
                subscriptions.get(sessionId).remove(message.workspaceId);
                LOGGER.debug("Session {} unsubscribed from workspace: {}", sessionId, message.workspaceId);
            }
        } catch (Exception exception) {
            LOGGER.error("Error handling WebSocket message", exception);
        }
    }

    /**
     * Broadcast a realtime event to all connected clients subscribed to the event's workspace.
     * Called by EventRealtimeBroadcaster listener.
     */
    public void broadcastEvent(RealtimeEvent event) {
        RealtimeMessage message = new RealtimeMessage(
            "event",
            event.workspaceId(),
            event.eventType(),
            event,
            event.version()
        );

        sessions.forEach((sessionId, state) -> {
            Set<String> sessionSubscriptions = subscriptions.get(sessionId);
            if (sessionSubscriptions == null) {
                sessionSubscriptions = Collections.emptySet();
            }

            // Broadcast if:
            // 1. Client is subscribed to the specific workspace, OR
            // 2. workspaceId is null (tenant-wide broadcast) and client has any subscription for this tenant
            boolean shouldBroadcast = (event.workspaceId() != null && sessionSubscriptions.contains(event.workspaceId())) ||
                                    (event.workspaceId() == null && !sessionSubscriptions.isEmpty());

            if (shouldBroadcast) {
                Sinks.EmitResult result = state.outboundMessages().tryEmitNext(message);
                if (result.isSuccess()) {
                    LOGGER.debug("Event broadcasted to session {}: type={}", sessionId, event.eventType());
                } else {
                    LOGGER.warn("Error queueing WebSocket message for session {}: {}", sessionId, result);
                }
            }
        });
    }

    private String serialize(RealtimeMessage message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to serialize realtime message", exception);
        }
    }

    /**
     * Get number of active sessions.
     */
    public int getActiveSessionCount() {
        return sessions.size();
    }

    /**
     * Get subscriptions for a workspace.
     */
    public int getSubscriberCountForWorkspace(String workspaceId) {
        return (int) subscriptions.values().stream()
                .filter(subs -> subs.contains(workspaceId))
                .count();
    }

    /**
     * Message format for WebSocket communication.
     */
    public static class RealtimeMessage {
        public String action;
        public String workspaceId;
        public String eventType;
        public Long version;
        public Object data;

        public RealtimeMessage() {}

        public RealtimeMessage(String action, String workspaceId, String eventType, Object data) {
            this(action, workspaceId, eventType, data, null);
        }

        public RealtimeMessage(String action, String workspaceId, String eventType, Object data, Long version) {
            this.action = action;
            this.workspaceId = workspaceId;
            this.eventType = eventType;
            this.version = version;
            this.data = data;
        }
    }

    private record SessionState(Sinks.Many<RealtimeMessage> outboundMessages) {
    }
}
