package com.example.platform.identityaccess.application.authorization;

import com.example.platform.identityaccess.domain.MembershipEntity;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class OpaLocalAuthorizationPolicyEngine implements AuthorizationPolicyEngine {

    private final AbacOpaAuthorizationDecisionUseCase abacOpaAuthorization;

    public OpaLocalAuthorizationPolicyEngine(AbacOpaAuthorizationDecisionUseCase abacOpaAuthorization) {
        this.abacOpaAuthorization = abacOpaAuthorization;
    }

    @Override
    public String name() {
        return "opa-local";
    }

    @Override
    public AuthorizationDecision decide(MembershipEntity membership, AuthorizationDecisionRequest request) {
        AuthorizationDecision decision = abacOpaAuthorization.decide(membership, request);
        return new AuthorizationDecision(
                "engine-v2",
                name(),
                "local-opa-compatible-workspace-policy-v1",
                decision.allowed(),
                decision.reason(),
                decision.actorUserId(),
                decision.tenantId(),
                decision.workspaceId(),
                decision.role(),
                decision.action(),
                decision.resourceType(),
                decision.resourceId(),
                decision.matchedRules(),
                Instant.now()
        );
    }
}
