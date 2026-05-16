package com.example.platform.common.web;

import com.example.platform.common.domain.DomainEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * WebSocket handler for realtime event broadcasts.
 * Phase 2: Manages active WebSocket connections and broadcasts events to subscribed clients.
 *
 * Each session can subscribe to events for their workspace/tenant.
 * Events are broadcast to connected clients in real-time via WebSocket.
 */
@Component
public class RealtimeEventHandler extends TextWebSocketHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(RealtimeEventHandler.class);

    private final ObjectMapper objectMapper;
    // Map of sessionId -> WebSocketSession
    private final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    // Map of sessionId -> Set of subscribed workspaceIds
    private final ConcurrentHashMap<String, Set<String>> subscriptions = new ConcurrentHashMap<>();

    public RealtimeEventHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = session.getId();
        sessions.put(sessionId, session);
        subscriptions.put(sessionId, Collections.synchronizedSet(new HashSet<>()));

        LOGGER.info("WebSocket connection established: {}", sessionId);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String content = message.getPayload();
        String sessionId = session.getId();

        try {
            // Parse subscription messages like {"action": "subscribe", "workspaceId": "workspace-123"}
            RealtimeMessage rtMsg = objectMapper.readValue(content, RealtimeMessage.class);

            if ("subscribe".equals(rtMsg.action)) {
                subscriptions.get(sessionId).add(rtMsg.workspaceId);
                LOGGER.debug("Session {} subscribed to workspace: {}", sessionId, rtMsg.workspaceId);

                // Send confirmation
                sendMessage(session, new RealtimeMessage(
                    "subscribed",
                    rtMsg.workspaceId,
                    null,
                    "Successfully subscribed to workspace"
                ));
            } else if ("unsubscribe".equals(rtMsg.action)) {
                subscriptions.get(sessionId).remove(rtMsg.workspaceId);
                LOGGER.debug("Session {} unsubscribed from workspace: {}", sessionId, rtMsg.workspaceId);
            }
        } catch (Exception e) {
            LOGGER.error("Error handling WebSocket message", e);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String sessionId = session.getId();
        sessions.remove(sessionId);
        subscriptions.remove(sessionId);
        LOGGER.info("WebSocket connection closed: {} ({})", sessionId, status);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        LOGGER.error("WebSocket transport error for session {}", session.getId(), exception);
    }

    /**
     * Broadcast a domain event to all connected clients subscribed to the event's workspace.
     * Called by EventRealtimeBroadcaster listener.
     */
    public void broadcastEvent(String workspaceId, String tenantId, DomainEvent event) {
        RealtimeMessage msg = new RealtimeMessage(
            "event",
            workspaceId,
            event.getEventType(),
            event
        );

        sessions.forEach((sessionId, session) -> {
            if (session.isOpen()) {
                Set<String> sessionSubscriptions = subscriptions.get(sessionId);
                if (sessionSubscriptions == null) {
                    sessionSubscriptions = Collections.emptySet();
                }

                // Broadcast if:
                // 1. Client is subscribed to the specific workspace, OR
                // 2. workspaceId is null (tenant-wide broadcast) and client has any subscription for this tenant
                boolean shouldBroadcast = (workspaceId != null && sessionSubscriptions.contains(workspaceId)) ||
                                        (workspaceId == null && !sessionSubscriptions.isEmpty());

                if (shouldBroadcast) {
                    try {
                        sendMessage(session, msg);
                        LOGGER.debug("Event broadcasted to session {}: type={}", sessionId, event.getEventType());
                    } catch (IOException e) {
                        LOGGER.warn("Error sending message to session {}", sessionId, e);
                    }
                }
            }
        });
    }

    private void sendMessage(WebSocketSession session, RealtimeMessage message) throws IOException {
        String json = objectMapper.writeValueAsString(message);
        session.sendMessage(new TextMessage(json));
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
        public Object data;

        public RealtimeMessage() {}

        public RealtimeMessage(String action, String workspaceId, String eventType, Object data) {
            this.action = action;
            this.workspaceId = workspaceId;
            this.eventType = eventType;
            this.data = data;
        }
    }
}
