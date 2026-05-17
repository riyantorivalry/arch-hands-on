package com.example.platform.identityaccess.application.authorization;

import com.example.platform.identityaccess.domain.MembershipEntity;

public interface AuthorizationPolicyEngine {

    String name();

    AuthorizationDecision decide(MembershipEntity membership, AuthorizationDecisionRequest request);
}
