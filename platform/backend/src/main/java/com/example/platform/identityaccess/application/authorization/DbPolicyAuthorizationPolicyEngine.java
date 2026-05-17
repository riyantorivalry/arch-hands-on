package com.example.platform.identityaccess.application.authorization;

import com.example.platform.identityaccess.domain.AuthorizationPolicyRuleEntity;
import com.example.platform.identityaccess.domain.MembershipEntity;
import com.example.platform.identityaccess.domain.MembershipStatus;
import com.example.platform.identityaccess.infrastructure.AuthorizationPolicyRuleRepository;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class DbPolicyAuthorizationPolicyEngine implements AuthorizationPolicyEngine {

    private static final String ALLOW = "ALLOW";
    private static final String DENY = "DENY";

    private final AuthorizationPolicyRuleRepository ruleRepository;

    public DbPolicyAuthorizationPolicyEngine(AuthorizationPolicyRuleRepository ruleRepository) {
        this.ruleRepository = ruleRepository;
    }

    @Override
    public String name() {
        return "db-policy";
    }

    @Override
    public AuthorizationDecision decide(MembershipEntity membership, AuthorizationDecisionRequest request) {
        String action = requireAction(request.action());
        if (membership.getStatus() != MembershipStatus.ACTIVE) {
            return decision(membership, request, action, false, "Membership is not active", List.of("db.deny.inactive_membership"));
        }
        if (!sameTenant(membership, request)) {
            return decision(membership, request, action, false, "Resource tenant does not match actor tenant", List.of("db.deny.cross_tenant_resource"));
        }

        for (AuthorizationPolicyRuleEntity rule : ruleRepository.findByEnabledTrueOrderByPriorityAscRuleIdAsc()) {
            if (matches(rule, membership, request, action)) {
                boolean allowed = ALLOW.equals(rule.getEffect());
                String reason = allowed
                        ? "Allowed by database policy rule " + rule.getRuleId()
                        : "Denied by database policy rule " + rule.getRuleId();
                return decision(membership, request, action, allowed, reason, List.of(rule.getRuleId()));
            }
        }

        return decision(membership, request, action, false, "No database policy rule allowed this action", List.of("db.deny.no_matching_rule"));
    }

    private boolean matches(
            AuthorizationPolicyRuleEntity rule,
            MembershipEntity membership,
            AuthorizationDecisionRequest request,
            String action
    ) {
        return matchesValue(rule.getRoleName(), membership.getRole().name())
                && matchesValue(rule.getAction(), action)
                && matchesValue(rule.getResourceType(), resourceType(request))
                && conditionMatches(rule.getConditionType(), membership, request);
    }

    private boolean matchesValue(String pattern, String value) {
        return "*".equals(pattern) || pattern.equalsIgnoreCase(value == null ? "" : value);
    }

    private boolean conditionMatches(String conditionType, MembershipEntity membership, AuthorizationDecisionRequest request) {
        return switch (conditionType.toUpperCase(Locale.ROOT)) {
            case "NONE" -> true;
            case "HIGH_RISK" -> riskLevel(request).equals("HIGH");
            case "RESOURCE_OWNER" -> membership.getUserId().equals(request.resourceOwnerUserId());
            case "RESOURCE_OWNER_OR_ASSIGNEE" ->
                    membership.getUserId().equals(request.resourceOwnerUserId())
                            || membership.getUserId().equals(request.assigneeUserId());
            default -> false;
        };
    }

    private AuthorizationDecision decision(
            MembershipEntity membership,
            AuthorizationDecisionRequest request,
            String action,
            boolean allowed,
            String reason,
            List<String> matchedRules
    ) {
        return new AuthorizationDecision(
                "engine-v4",
                name(),
                "db-workspace-policy-v1",
                allowed,
                reason,
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

    private String resourceType(AuthorizationDecisionRequest request) {
        String resourceType = request.resourceType();
        return resourceType == null || resourceType.isBlank() ? "*" : resourceType.trim();
    }

    private String riskLevel(AuthorizationDecisionRequest request) {
        Map<String, Object> attributes = request.attributes();
        if (attributes == null) {
            return "";
        }
        Object riskLevel = attributes.get("riskLevel");
        return riskLevel == null ? "" : riskLevel.toString().trim().toUpperCase(Locale.ROOT);
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
}
