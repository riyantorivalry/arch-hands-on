package com.example.platform.identityaccess.application.authorization;

import java.time.Instant;
import java.util.List;

public record AuthorizationDecision(
        String strategyVersion,
        String strategy,
        String policyId,
        boolean allowed,
        String reason,
        String actorUserId,
        String tenantId,
        String workspaceId,
        String role,
        String action,
        String resourceType,
        String resourceId,
        List<String> matchedRules,
        Instant evaluatedAt
) {
}
