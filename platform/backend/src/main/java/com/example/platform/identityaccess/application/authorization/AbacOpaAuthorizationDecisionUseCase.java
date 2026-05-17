package com.example.platform.identityaccess.application.authorization;

import com.example.platform.identityaccess.domain.MembershipEntity;
import com.example.platform.identityaccess.domain.MembershipRole;
import com.example.platform.identityaccess.domain.MembershipStatus;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class AbacOpaAuthorizationDecisionUseCase implements AuthorizationDecisionUseCase {

    @Override
    public AuthorizationDecision decide(MembershipEntity membership, AuthorizationDecisionRequest request) {
        String action = requireAction(request.action());
        List<String> matchedRules = new ArrayList<>();

        DecisionResult result = evaluate(membership, request, action, matchedRules);

        return new AuthorizationDecision(
                "v2",
                "abac-opa",
                "local-opa-compatible-workspace-policy-v1",
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
            matchedRules.add("deny.inactive_membership");
            return new DecisionResult(false, "Membership is not active");
        }
        if (!sameTenant(membership, request)) {
            matchedRules.add("deny.cross_tenant_resource");
            return new DecisionResult(false, "Resource tenant does not match actor tenant");
        }
        if (riskLevel(request).equals("HIGH") && !isManager(membership)) {
            matchedRules.add("deny.high_risk_requires_manager");
            return new DecisionResult(false, "High-risk actions require OWNER or ADMIN");
        }
        if (isManager(membership)) {
            matchedRules.add("allow.workspace_manager");
            return new DecisionResult(true, "Allowed by manager role and tenant attributes");
        }
        if (isReadAction(action)) {
            matchedRules.add("allow.member_read");
            return new DecisionResult(true, "Allowed read for active workspace member");
        }
        if (action.equals("document:create") || action.equals("task:create") || action.equals("message:post")) {
            matchedRules.add("allow.member_create");
            return new DecisionResult(true, "Allowed create for active workspace member");
        }
        if (action.equals("document:update") && ownsResource(membership, request)) {
            matchedRules.add("allow.resource_owner_document_update");
            return new DecisionResult(true, "Allowed because actor owns the document");
        }
        if (action.equals("task:update") && (ownsResource(membership, request) || isAssignee(membership, request))) {
            matchedRules.add("allow.owner_or_assignee_task_update");
            return new DecisionResult(true, "Allowed because actor owns or is assigned to the task");
        }

        matchedRules.add("deny.no_matching_allow_rule");
        return new DecisionResult(false, "No ABAC/OPA policy rule allowed this action");
    }

    private boolean sameTenant(MembershipEntity membership, AuthorizationDecisionRequest request) {
        String resourceTenantId = request.resourceTenantId();
        return resourceTenantId == null || resourceTenantId.isBlank() || membership.getTenantId().equals(resourceTenantId);
    }

    private boolean isManager(MembershipEntity membership) {
        return membership.getRole() == MembershipRole.OWNER || membership.getRole() == MembershipRole.ADMIN;
    }

    private boolean isReadAction(String action) {
        return action.endsWith(":read") || action.equals("document:search") || action.equals("workspace:read");
    }

    private boolean ownsResource(MembershipEntity membership, AuthorizationDecisionRequest request) {
        return membership.getUserId().equals(request.resourceOwnerUserId());
    }

    private boolean isAssignee(MembershipEntity membership, AuthorizationDecisionRequest request) {
        return membership.getUserId().equals(request.assigneeUserId());
    }

    private String riskLevel(AuthorizationDecisionRequest request) {
        Map<String, Object> attributes = request.attributes();
        if (attributes == null) {
            return "";
        }
        Object riskLevel = attributes.get("riskLevel");
        return riskLevel == null ? "" : riskLevel.toString().trim().toUpperCase();
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
