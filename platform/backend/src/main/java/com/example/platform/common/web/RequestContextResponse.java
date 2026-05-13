package com.example.platform.common.web;

public record RequestContextResponse(String tenantId, String workspaceId, String userId, String correlationId) {

    public static RequestContextResponse from(RequestContext context) {
        return new RequestContextResponse(
                context.tenantId(),
                context.workspaceId(),
                context.userId(),
                context.correlationId()
        );
    }
}
