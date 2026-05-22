package com.example.platform.identityaccess.api;

import com.example.platform.common.web.RequestContextResponse;
import com.example.platform.common.web.RequestContexts;
import com.example.platform.identityaccess.application.IdentityAccessFacade;
import com.example.platform.identityaccess.application.SessionAuthenticationService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Validated
@RestController
@RequestMapping("/api")
public class IdentityAccessController {

    private final IdentityAccessFacade facade;
    private final SessionAuthenticationService sessionAuthenticationService;

    public IdentityAccessController(IdentityAccessFacade facade, SessionAuthenticationService sessionAuthenticationService) {
        this.facade = facade;
        this.sessionAuthenticationService = sessionAuthenticationService;
    }

    @PostMapping("/auth/login")
    public Mono<LoginResponse> login(@RequestBody LoginRequest request) {
        return sessionAuthenticationService.login(request.userId(), request.workspaceId())
                .map(session -> new LoginResponse(
                        session.token(),
                        session.userId(),
                        session.tenantId(),
                        session.workspaceId(),
                        session.expiresAt().toString()
                ));
    }

    @PostMapping("/auth/logout")
    public Mono<LogoutResponse> logout(@org.springframework.web.bind.annotation.RequestHeader("Authorization") String authorization) {
        return sessionAuthenticationService.logout(extractToken(authorization))
                .thenReturn(new LogoutResponse("logged-out"));
    }

    @GetMapping("/me")
    public Mono<MeResponse> me() {
        return RequestContexts.authenticatedReactive()
                .flatMap(context -> facade.getCurrentActor(context.workspaceId(), context.userId()))
                .map(actor -> new MeResponse(
                        actor.userId(),
                        actor.displayName(),
                        actor.email(),
                        actor.workspaceId(),
                        actor.tenantId(),
                        actor.workspaceRole()
                ));
    }

    @GetMapping("/workspaces/{workspaceId}/memberships/me")
    public Mono<MembershipResponse> membership(@PathVariable String workspaceId) {
        return RequestContexts.authenticatedReactive()
                .flatMap(context -> facade.getCurrentActor(workspaceId, context.userId()))
                .map(actor -> new MembershipResponse(actor.userId(), actor.workspaceId(), actor.tenantId(), actor.workspaceRole()));
    }

    @PostMapping("/workspaces/{workspaceId}/memberships")
    public Mono<IdentityAccessFacade.MembershipAssignmentView> assignMembership(
            @PathVariable String workspaceId,
            @RequestBody AssignMembershipRequest request
    ) {
        return RequestContexts.authenticatedReactive()
                .flatMap(context -> facade.assignMembership(
                        context.userId(),
                        workspaceId,
                        request.userId(),
                        request.email(),
                        request.displayName(),
                        request.role()
                ));
    }

    public record LoginRequest(@NotBlank String userId, @NotBlank String workspaceId) {
    }

    public record LoginResponse(String token, String userId, String tenantId, String workspaceId, String expiresAt) {
    }

    public record LogoutResponse(String status) {
    }

    public record MeResponse(
            String userId,
            String displayName,
            String email,
            String workspaceId,
            String tenantId,
            String role
    ) {
    }

    public record MembershipResponse(String userId, String workspaceId, String tenantId, String role) {
    }

    public record AssignMembershipRequest(
            @NotBlank String userId,
            @NotBlank String email,
            @NotBlank String displayName,
            @NotBlank String role
    ) {
    }

    private String extractToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new com.example.platform.common.web.AuthenticationRequiredException("Bearer token is required");
        }
        return authorization.substring("Bearer ".length()).trim();
    }
}
