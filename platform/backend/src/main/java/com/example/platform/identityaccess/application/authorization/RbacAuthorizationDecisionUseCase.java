package com.example.platform.identityaccess.application.authorization;

import com.example.platform.identityaccess.domain.MembershipEntity;
import com.example.platform.identityaccess.domain.MembershipRole;
import com.example.platform.identityaccess.domain.MembershipStatus;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class RbacAuthorizationDecisionUseCase implements AuthorizationDecisionUseCase {

    private static final Set<String> MANAGER_ACTIONS = Set.of(
            "workspace:create",
            "workspace:update_settings",
            "membership:assign",
            "channel:create",
            "analytics:read"
    );

    private static final Set<String> MEMBER_ACTIONS = Set.of(
            "workspace:read",
            "document:read",
            "document:search",
            "document:create",
            "message:post",
            "message:read",
            "task:read",
            "task:create"
    );

    private static final Map<MembershipRole, List<String>> ROLE_RULES = Map.of(
            MembershipRole.OWNER, List.of("role:OWNER"),
            MembershipRole.ADMIN, List.of("role:ADMIN"),
            MembershipRole.MEMBER, List.of("role:MEMBER")
    );

    @Override
    public AuthorizationDecision decide(MembershipEntity membership, AuthorizationDecisionRequest request) {
        String action = normalizeAction(request.action());
        boolean active = membership.getStatus() == MembershipStatus.ACTIVE;
        boolean manager = membership.getRole() == MembershipRole.OWNER || membership.getRole() == MembershipRole.ADMIN;
        boolean allowed = active && (manager || MEMBER_ACTIONS.contains(action)) && !requiresManager(action);
        if (active && manager && (MANAGER_ACTIONS.contains(action) || MEMBER_ACTIONS.contains(action) || action.endsWith(":update"))) {
            allowed = true;
        }

        String reason;
        if (!active) {
            reason = "Membership is not active";
        } else if (allowed) {
            reason = "Allowed by workspace role " + membership.getRole();
        } else {
            reason = "RBAC role " + membership.getRole() + " is not allowed to perform " + action;
        }

        return new AuthorizationDecision(
                "v1",
                "rbac",
                "rbac-workspace-roles-v1",
                allowed,
                reason,
                membership.getUserId(),
                membership.getTenantId(),
                membership.getWorkspaceId(),
                membership.getRole().name(),
                action,
                blankToNull(request.resourceType()),
                blankToNull(request.resourceId()),
                ROLE_RULES.getOrDefault(membership.getRole(), List.of()),
                Instant.now()
        );
    }

    private boolean requiresManager(String action) {
        return MANAGER_ACTIONS.contains(action);
    }

    private String normalizeAction(String action) {
        if (action == null || action.isBlank()) {
            throw new IllegalArgumentException("action is required");
        }
        return action.trim();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
