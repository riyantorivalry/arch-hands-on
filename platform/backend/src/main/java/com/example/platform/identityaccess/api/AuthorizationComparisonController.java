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
import reactor.core.publisher.Mono;

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
    public Mono<AuthorizationDecision> decideWithRbac(
            @PathVariable String workspaceId,
            @RequestBody AuthorizationDecisionRequest request
    ) {
        return requireActiveMembership(workspaceId).map(membership -> rbacAuthorization.decide(membership, request));
    }

    @PostMapping("/v2/workspaces/{workspaceId}/authorization/decisions")
    public Mono<AuthorizationDecision> decideWithAbacOpa(
            @PathVariable String workspaceId,
            @RequestBody AuthorizationDecisionRequest request
    ) {
        return requireActiveMembership(workspaceId).map(membership -> abacOpaAuthorization.decide(membership, request));
    }

    @PostMapping("/workspaces/{workspaceId}/authorization/decisions")
    public Mono<AuthorizationDecision> decideWithSelectedPolicyEngine(
            @PathVariable String workspaceId,
            @RequestBody AuthorizationDecisionRequest request
    ) {
        return requireActiveMembership(workspaceId)
                .flatMap(membership -> policyEngineService.decideReactive(membership, request));
    }

    @PostMapping("/benchmarks/authorization/{engine}/decisions")
    public Mono<AuthorizationDecision> decideWithPolicyEngine(
            @PathVariable String engine,
            @RequestBody AuthorizationDecisionRequest request
    ) {
        return RequestContexts.authenticatedReactive()
                .flatMap(context -> requireActiveMembership(context.workspaceId())
                        .flatMap(membership -> policyEngineService.decideReactive(engine, membership, request)));
    }

    @org.springframework.web.bind.annotation.GetMapping("/benchmarks/authorization/engine")
    public Mono<AuthorizationEngineResponse> selectedPolicyEngine() {
        return RequestContexts.authenticatedReactive()
                .thenReturn(new AuthorizationEngineResponse(policyEngineService.mode()));
    }

    public record AuthorizationEngineResponse(String selectedEngine) {
    }

    private Mono<MembershipEntity> requireActiveMembership(String workspaceId) {
        return RequestContexts.authenticatedReactive()
                .flatMap(context -> membershipRepository.findByWorkspaceIdAndUserId(workspaceId, context.userId())
                        .switchIfEmpty(Mono.error(new AuthorizationDeniedException("User is not a member of workspace " + workspaceId)))
                        .flatMap(membership -> {
                            if (membership.getStatus() != MembershipStatus.ACTIVE) {
                                return Mono.error(new IllegalStateException("Membership is not active for user " + context.userId()));
                            }
                            return Mono.just(membership);
                        }));
    }
}
