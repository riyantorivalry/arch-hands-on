package com.example.platform.common.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket configuration for realtime updates.
 * Keeps the original endpoint and adds the versioned comparison endpoint.
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
        registry.addHandler(realtimeEventHandler, "/ws/realtime", "/ws/v3/realtime")
                .setAllowedOrigins("*")
                .withSockJS();
    }
}

