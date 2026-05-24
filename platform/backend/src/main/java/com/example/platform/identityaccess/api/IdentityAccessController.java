package com.example.platform.identityaccess.api;

import com.example.platform.common.web.RequestContextResponse;
import com.example.platform.common.web.RequestContexts;
import com.example.platform.identityaccess.application.AuthenticationClient;
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
    public LoginResponse login(@RequestBody LoginRequest request) {
        var session = sessionAuthenticationService.login(
                request.userId(),
                request.workspaceId(),
                new AuthenticationClient(request.clientId(), request.clientType())
        );
        return LoginResponse.from(session);
    }

    @PostMapping("/auth/refresh")
    public LoginResponse refresh(@RequestBody RefreshRequest request) {
        var session = sessionAuthenticationService.refresh(request.refreshToken());
        return LoginResponse.from(session);
    }

    @PostMapping("/auth/logout")
    public LogoutResponse logout(@org.springframework.web.bind.annotation.RequestHeader("Authorization") String authorization) {
        sessionAuthenticationService.logout(extractToken(authorization));
        return new LogoutResponse("logged-out");
    }

    @GetMapping("/me")
    public MeResponse me() {
        var context = RequestContexts.authenticated();
        var actor = facade.getCurrentActor(context.workspaceId(), context.userId());
        return new MeResponse(
                actor.userId(),
                actor.displayName(),
                actor.email(),
                actor.workspaceId(),
                actor.tenantId(),
                actor.workspaceRole()
        );
    }

    @GetMapping("/workspaces/{workspaceId}/memberships/me")
    public MembershipResponse membership(@PathVariable String workspaceId) {
        var context = RequestContexts.authenticated();
        var actor = facade.getCurrentActor(workspaceId, context.userId());
        return new MembershipResponse(actor.userId(), actor.workspaceId(), actor.tenantId(), actor.workspaceRole());
    }

    @PostMapping("/workspaces/{workspaceId}/memberships")
    public IdentityAccessFacade.MembershipAssignmentView assignMembership(
            @PathVariable String workspaceId,
            @RequestBody AssignMembershipRequest request
    ) {
        var context = RequestContexts.authenticated();
        return facade.assignMembership(
                context.userId(),
                workspaceId,
                request.userId(),
                request.email(),
                request.displayName(),
                request.role()
        );
    }

    public record LoginRequest(
            @NotBlank String userId,
            @NotBlank String workspaceId,
            String clientId,
            AuthenticationClient.ClientType clientType
    ) {
    }

    public record RefreshRequest(@NotBlank String refreshToken) {
    }

    public record LoginResponse(
            String token,
            String accessToken,
            String refreshToken,
            String tokenType,
            long expiresIn,
            String sessionId,
            String userId,
            String tenantId,
            String workspaceId,
            String clientId,
            String clientType,
            String expiresAt,
            String refreshExpiresAt
    ) {
        private static LoginResponse from(SessionAuthenticationService.AuthenticatedSession session) {
            var tokens = session.tokens();
            return new LoginResponse(
                    tokens.accessToken(),
                    tokens.accessToken(),
                    tokens.refreshToken(),
                    tokens.tokenType(),
                    tokens.expiresIn(),
                    session.sessionId(),
                    session.userId(),
                    session.tenantId(),
                    session.workspaceId(),
                    session.clientId(),
                    session.clientType(),
                    tokens.accessTokenExpiresAt().toString(),
                    tokens.refreshTokenExpiresAt().toString()
            );
        }
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
        if (authorization == null || !authorization.regionMatches(true, 0, "Bearer ", 0, "Bearer ".length())) {
            throw new com.example.platform.common.web.AuthenticationRequiredException("Bearer token is required");
        }
        return authorization.substring("Bearer ".length()).trim();
    }
}
