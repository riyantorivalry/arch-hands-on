package com.example.platform.common.web;

public record RequestContext(String tenantId, String workspaceId, String userId, String correlationId) {
}
