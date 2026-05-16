package com.example.platform.common.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket configuration for Phase 2 realtime updates.
 * Enables /ws endpoint for WebSocket connections with SockJS fallback.
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final RealtimeEventHandler realtimeEventHandler;

    public WebSocketConfig(RealtimeEventHandler realtimeEventHandler) {
        this.realtimeEventHandler = realtimeEventHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(realtimeEventHandler, "/ws/realtime")
                .setAllowedOrigins("*")
                .withSockJS();
    }
}

