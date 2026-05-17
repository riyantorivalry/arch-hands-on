package com.example.platform.identityaccess.application.authorization;

import com.example.platform.identityaccess.domain.MembershipEntity;

public interface AuthorizationDecisionUseCase {

    AuthorizationDecision decide(MembershipEntity membership, AuthorizationDecisionRequest request);
}
