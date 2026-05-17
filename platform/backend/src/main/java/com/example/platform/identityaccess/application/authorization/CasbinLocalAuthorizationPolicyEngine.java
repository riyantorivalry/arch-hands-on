package com.example.platform.identityaccess.application.authorization;

import com.example.platform.identityaccess.domain.MembershipEntity;
import com.example.platform.identityaccess.domain.MembershipRole;
import com.example.platform.identityaccess.domain.MembershipStatus;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class CasbinLocalAuthorizationPolicyEngine implements AuthorizationPolicyEngine {

    @Override
    public String name() {
        return "casbin-local";
    }

    @Override
    public AuthorizationDecision decide(MembershipEntity membership, AuthorizationDecisionRequest request) {
        String action = requireAction(request.action());
        List<String> matchedRules = new ArrayList<>();
        DecisionResult result = evaluate(membership, request, action, matchedRules);
        return new AuthorizationDecision(
                "engine-v3",
                name(),
                "local-casbin-rbac-abac-policy-v1",
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

    private DecisionResult evaluate(
            MembershipEntity membership,
            AuthorizationDecisionRequest request,
            String action,
            List<String> matchedRules
    ) {
        if (membership.getStatus() != MembershipStatus.ACTIVE) {
            matchedRules.add("casbin.deny.inactive_membership");
            return new DecisionResult(false, "Membership is not active");
        }
        if (!sameTenant(membership, request)) {
            matchedRules.add("casbin.deny.cross_tenant_resource");
            return new DecisionResult(false, "Resource tenant does not match actor tenant");
        }
        if (isManager(membership)) {
            matchedRules.add("p, manager, *, *");
            return new DecisionResult(true, "Allowed by Casbin-style manager policy");
        }
        if (riskLevel(request).equals("HIGH")) {
            matchedRules.add("casbin.deny.high_risk_requires_manager");
            return new DecisionResult(false, "High-risk actions require OWNER or ADMIN");
        }
        if (isMemberBaselineAction(action)) {
            matchedRules.add("p, member, " + object(request) + ", " + action);
            return new DecisionResult(true, "Allowed by Casbin-style member policy");
        }
        if (action.equals("document:update") && membership.getUserId().equals(request.resourceOwnerUserId())) {
            matchedRules.add("p, member, document, update, owner");
            return new DecisionResult(true, "Allowed by Casbin-style owner condition");
        }
        if (action.equals("task:update")
                && (membership.getUserId().equals(request.resourceOwnerUserId())
                || membership.getUserId().equals(request.assigneeUserId()))) {
            matchedRules.add("p, member, task, update, owner_or_assignee");
            return new DecisionResult(true, "Allowed by Casbin-style owner/assignee condition");
        }
        matchedRules.add("casbin.deny.no_matching_policy");
        return new DecisionResult(false, "No Casbin-style policy allowed this action");
    }

    private boolean sameTenant(MembershipEntity membership, AuthorizationDecisionRequest request) {
        String resourceTenantId = request.resourceTenantId();
        return resourceTenantId == null || resourceTenantId.isBlank() || membership.getTenantId().equals(resourceTenantId);
    }

    private boolean isManager(MembershipEntity membership) {
        return membership.getRole() == MembershipRole.OWNER || membership.getRole() == MembershipRole.ADMIN;
    }

    private boolean isMemberBaselineAction(String action) {
        return action.endsWith(":read")
                || action.equals("workspace:read")
                || action.equals("document:search")
                || action.equals("document:create")
                || action.equals("task:create")
                || action.equals("message:post")
                || action.equals("message:read");
    }

    private String riskLevel(AuthorizationDecisionRequest request) {
        Map<String, Object> attributes = request.attributes();
        if (attributes == null) {
            return "";
        }
        Object riskLevel = attributes.get("riskLevel");
        return riskLevel == null ? "" : riskLevel.toString().trim().toUpperCase();
    }

    private String object(AuthorizationDecisionRequest request) {
        String resourceType = request.resourceType();
        return resourceType == null || resourceType.isBlank() ? "*" : resourceType.trim();
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
