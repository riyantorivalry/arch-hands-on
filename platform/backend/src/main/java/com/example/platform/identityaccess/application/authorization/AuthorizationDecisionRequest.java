package com.example.platform.identityaccess.application.authorization;

import java.util.Map;

public record AuthorizationDecisionRequest(
        String action,
        String resourceType,
        String resourceId,
        String resourceTenantId,
        String resourceOwnerUserId,
        String assigneeUserId,
        Map<String, Object> attributes
) {
}
