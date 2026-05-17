package com.example.platform.identityaccess.api;

import com.example.platform.common.web.AuthorizationDeniedException;
import com.example.platform.common.web.RequestContexts;
import com.example.platform.identityaccess.application.authorization.AbacOpaAuthorizationDecisionUseCase;
import com.example.platform.identityaccess.application.authorization.AuthorizationDecision;
import com.example.platform.identityaccess.application.authorization.AuthorizationDecisionRequest;
import com.example.platform.identityaccess.application.authorization.AuthorizationPolicyEngineService;
import com.example.platform.identityaccess.application.authorization.RbacAuthorizationDecisionUseCase;
import com.example.platform.identityaccess.domain.MembershipEntity;
import com.example.platform.identityaccess.domain.MembershipStatus;
import com.example.platform.identityaccess.infrastructure.MembershipRepository;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api")
public class AuthorizationComparisonController {

    private final MembershipRepository membershipRepository;
    private final RbacAuthorizationDecisionUseCase rbacAuthorization;
    private final AbacOpaAuthorizationDecisionUseCase abacOpaAuthorization;
    private final AuthorizationPolicyEngineService policyEngineService;

    public AuthorizationComparisonController(
            MembershipRepository membershipRepository,
            RbacAuthorizationDecisionUseCase rbacAuthorization,
            AbacOpaAuthorizationDecisionUseCase abacOpaAuthorization,
            AuthorizationPolicyEngineService policyEngineService
    ) {
        this.membershipRepository = membershipRepository;
        this.rbacAuthorization = rbacAuthorization;
        this.abacOpaAuthorization = abacOpaAuthorization;
        this.policyEngineService = policyEngineService;
    }

    @PostMapping("/v1/workspaces/{workspaceId}/authorization/decisions")
    public AuthorizationDecision decideWithRbac(
            @PathVariable String workspaceId,
            @RequestBody AuthorizationDecisionRequest request
    ) {
        return rbacAuthorization.decide(requireActiveMembership(workspaceId), request);
    }

    @PostMapping("/v2/workspaces/{workspaceId}/authorization/decisions")
    public AuthorizationDecision decideWithAbacOpa(
            @PathVariable String workspaceId,
            @RequestBody AuthorizationDecisionRequest request
    ) {
        return abacOpaAuthorization.decide(requireActiveMembership(workspaceId), request);
    }

    @PostMapping("/workspaces/{workspaceId}/authorization/decisions")
    public AuthorizationDecision decideWithSelectedPolicyEngine(
            @PathVariable String workspaceId,
            @RequestBody AuthorizationDecisionRequest request
    ) {
        return policyEngineService.decide(requireActiveMembership(workspaceId), request);
    }

    @PostMapping("/benchmarks/authorization/{engine}/decisions")
    public AuthorizationDecision decideWithPolicyEngine(
            @PathVariable String engine,
            @RequestBody AuthorizationDecisionRequest request
    ) {
        String workspaceId = RequestContexts.authenticated().workspaceId();
        return policyEngineService.decide(engine, requireActiveMembership(workspaceId), request);
    }

    @org.springframework.web.bind.annotation.GetMapping("/benchmarks/authorization/engine")
    public AuthorizationEngineResponse selectedPolicyEngine() {
        RequestContexts.authenticated();
        return new AuthorizationEngineResponse(policyEngineService.mode());
    }

    public record AuthorizationEngineResponse(String selectedEngine) {
    }

    private MembershipEntity requireActiveMembership(String workspaceId) {
        String userId = RequestContexts.authenticated().userId();
        MembershipEntity membership = membershipRepository.findByWorkspaceIdAndUserId(workspaceId, userId)
                .orElseThrow(() -> new AuthorizationDeniedException("User is not a member of workspace " + workspaceId));
        if (membership.getStatus() != MembershipStatus.ACTIVE) {
            throw new IllegalStateException("Membership is not active for user " + userId);
        }
        return membership;
    }
}
