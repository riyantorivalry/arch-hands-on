package com.example.platform.identityaccess.application.authorization;

import com.example.platform.identityaccess.domain.MembershipEntity;
import com.example.platform.identityaccess.domain.MembershipRole;
import com.example.platform.identityaccess.domain.MembershipStatus;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class InCodeAuthorizationPolicyEngine implements AuthorizationPolicyEngine {

    @Override
    public String name() {
        return "in-code";
    }

    @Override
    public AuthorizationDecision decide(MembershipEntity membership, AuthorizationDecisionRequest request) {
        String action = requireAction(request.action());
        List<String> matchedRules = new ArrayList<>();
        DecisionResult result = evaluate(membership, request, action, matchedRules);
        return decision(membership, request, action, result, matchedRules);
    }

    private DecisionResult evaluate(
            MembershipEntity membership,
            AuthorizationDecisionRequest request,
            String action,
            List<String> matchedRules
    ) {
        if (membership.getStatus() != MembershipStatus.ACTIVE) {
            matchedRules.add("deny.inactive_membership");
            return new DecisionResult(false, "Membership is not active");
        }
        if (!sameTenant(membership, request)) {
            matchedRules.add("deny.cross_tenant_resource");
            return new DecisionResult(false, "Resource tenant does not match actor tenant");
        }
        if (isManager(membership)) {
            matchedRules.add("allow.owner_or_admin");
            return new DecisionResult(true, "Allowed by in-code OWNER/ADMIN guard");
        }
        if (isReadOrCreate(action)) {
            matchedRules.add("allow.member_read_or_create");
            return new DecisionResult(true, "Allowed by in-code active member guard");
        }
        if (action.equals("document:update") && membership.getUserId().equals(request.resourceOwnerUserId())) {
            matchedRules.add("allow.resource_owner");
            return new DecisionResult(true, "Allowed by in-code resource owner guard");
        }
        if (action.equals("task:update")
                && (membership.getUserId().equals(request.resourceOwnerUserId())
                || membership.getUserId().equals(request.assigneeUserId()))) {
            matchedRules.add("allow.resource_owner_or_assignee");
            return new DecisionResult(true, "Allowed by in-code task owner/assignee guard");
        }
        matchedRules.add("deny.no_matching_in_code_guard");
        return new DecisionResult(false, "No in-code authorization guard allowed this action");
    }

    private AuthorizationDecision decision(
            MembershipEntity membership,
            AuthorizationDecisionRequest request,
            String action,
            DecisionResult result,
            List<String> matchedRules
    ) {
        return new AuthorizationDecision(
                "engine-v1",
                name(),
                "in-code-application-guards-v1",
                result.allowed(),
                result.reason(),
                membership.getUserId(),
                membership.getTenantId(),
                membership.getWorkspaceId(),
                membership.getRole().name(),
                action,
                blankToNull(request.resourceType()),
                blankToNull(request.resourceId()),
                matchedRules,
                Instant.now()
        );
    }

    private boolean sameTenant(MembershipEntity membership, AuthorizationDecisionRequest request) {
        String resourceTenantId = request.resourceTenantId();
        return resourceTenantId == null || resourceTenantId.isBlank() || membership.getTenantId().equals(resourceTenantId);
    }

    private boolean isManager(MembershipEntity membership) {
        return membership.getRole() == MembershipRole.OWNER || membership.getRole() == MembershipRole.ADMIN;
    }

    private boolean isReadOrCreate(String action) {
        return action.endsWith(":read")
                || action.equals("document:search")
                || action.equals("document:create")
                || action.equals("task:create")
                || action.equals("message:post")
                || action.equals("message:read")
                || action.equals("workspace:read");
    }

    private String requireAction(String action) {
        if (action == null || action.isBlank()) {
            throw new IllegalArgumentException("action is required");
        }
        return action.trim();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private record DecisionResult(boolean allowed, String reason) {
    }
}
