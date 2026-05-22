package com.example.platform.identityaccess.application.authorization;

import com.example.platform.identityaccess.domain.MembershipEntity;
import reactor.core.publisher.Mono;

public interface AuthorizationPolicyEngine {

    String name();

    AuthorizationDecision decide(MembershipEntity membership, AuthorizationDecisionRequest request);

    default Mono<AuthorizationDecision> decideReactive(MembershipEntity membership, AuthorizationDecisionRequest request) {
        return Mono.fromSupplier(() -> decide(membership, request));
    }
}
