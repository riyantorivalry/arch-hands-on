package com.example.platform.identityaccess.application;

import com.example.platform.common.web.AuthorizationDeniedException;
import com.example.platform.identityaccess.application.authorization.AuthorizationDecision;
import com.example.platform.identityaccess.application.authorization.AuthorizationDecisionRequest;
import com.example.platform.identityaccess.application.authorization.AuthorizationPolicyEngineService;
import com.example.platform.identityaccess.domain.MembershipEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class AuthorizationService {

    private final AuthorizationPolicyEngineService policyEngineService;

    public AuthorizationService(AuthorizationPolicyEngineService policyEngineService) {
        this.policyEngineService = policyEngineService;
    }

    public Mono<Void> requireWorkspaceManager(MembershipEntity membership) {
        return requireAllowed(membership, new AuthorizationDecisionRequest(
                "workspace:manage",
                "workspace",
                membership.getWorkspaceId(),
                membership.getTenantId(),
                null,
                null,
                null
        ), "Workspace manager role is required");
    }

    public Mono<Void> requireOwnerOrAdminOrResourceOwner(MembershipEntity membership, String resourceOwnerUserId) {
        return requireAllowed(membership, new AuthorizationDecisionRequest(
                "document:update",
                "document",
                null,
                membership.getTenantId(),
                resourceOwnerUserId,
                null,
                null
        ), "Actor is not allowed to modify this resource");
    }

    public Mono<Void> requireOwnerOrAdminOrAssignee(
            MembershipEntity membership,
            String resourceOwnerUserId,
            String assigneeUserId
    ) {
        return requireAllowed(membership, new AuthorizationDecisionRequest(
                "task:update",
                "task",
                null,
                membership.getTenantId(),
                resourceOwnerUserId,
                assigneeUserId,
                null
        ), "Actor is not allowed to modify this task");
    }

    private Mono<Void> requireAllowed(MembershipEntity membership, AuthorizationDecisionRequest request, String denialMessage) {
        return policyEngineService.decideReactive(membership, request)
                .flatMap(decision -> decision.allowed()
                        ? Mono.empty()
                        : Mono.error(new AuthorizationDeniedException(denialMessage)));
    }
}
