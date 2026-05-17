package com.example.platform.identityaccess.application;

import com.example.platform.common.web.AuthorizationDeniedException;
import com.example.platform.identityaccess.application.authorization.AuthorizationDecision;
import com.example.platform.identityaccess.application.authorization.AuthorizationDecisionRequest;
import com.example.platform.identityaccess.application.authorization.AuthorizationPolicyEngineService;
import com.example.platform.identityaccess.domain.MembershipEntity;
import org.springframework.stereotype.Service;

@Service
public class AuthorizationService {

    private final AuthorizationPolicyEngineService policyEngineService;

    public AuthorizationService(AuthorizationPolicyEngineService policyEngineService) {
        this.policyEngineService = policyEngineService;
    }

    public void requireWorkspaceManager(MembershipEntity membership) {
        requireAllowed(membership, new AuthorizationDecisionRequest(
                "workspace:manage",
                "workspace",
                membership.getWorkspaceId(),
                membership.getTenantId(),
                null,
                null,
                null
        ), "Workspace manager role is required");
    }

    public void requireOwnerOrAdminOrResourceOwner(MembershipEntity membership, String resourceOwnerUserId) {
        requireAllowed(membership, new AuthorizationDecisionRequest(
                "document:update",
                "document",
                null,
                membership.getTenantId(),
                resourceOwnerUserId,
                null,
                null
        ), "Actor is not allowed to modify this resource");
    }

    public void requireOwnerOrAdminOrAssignee(
            MembershipEntity membership,
            String resourceOwnerUserId,
            String assigneeUserId
    ) {
        requireAllowed(membership, new AuthorizationDecisionRequest(
                "task:update",
                "task",
                null,
                membership.getTenantId(),
                resourceOwnerUserId,
                assigneeUserId,
                null
        ), "Actor is not allowed to modify this task");
    }

    private void requireAllowed(MembershipEntity membership, AuthorizationDecisionRequest request, String denialMessage) {
        AuthorizationDecision decision = policyEngineService.decide(membership, request);
        if (!decision.allowed()) {
            throw new AuthorizationDeniedException(denialMessage);
        }
    }
}
