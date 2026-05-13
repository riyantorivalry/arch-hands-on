package com.example.platform.common.web;

public record RequestContext(String tenantId, String workspaceId, String userId, String correlationId) {

    public boolean isAuthenticated() {
        return userId != null && !userId.isBlank();
    }
}
