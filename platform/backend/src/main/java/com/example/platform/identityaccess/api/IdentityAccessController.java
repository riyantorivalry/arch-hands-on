package com.example.platform.identityaccess.api;

import com.example.platform.common.web.RequestContextResponse;
import com.example.platform.common.web.RequestContexts;
import com.example.platform.identityaccess.application.IdentityAccessFacade;
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

    public IdentityAccessController(IdentityAccessFacade facade) {
        this.facade = facade;
    }

    @PostMapping("/auth/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        return new LoginResponse("stub-token", request.userId(), RequestContextResponse.from(RequestContexts.current()));
    }

    @PostMapping("/auth/logout")
    public LogoutResponse logout() {
        return new LogoutResponse("logged-out");
    }

    @GetMapping("/me")
    public MeResponse me() {
        var context = RequestContexts.current();
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
        var context = RequestContexts.current();
        var actor = facade.getCurrentActor(workspaceId, context.userId());
        return new MembershipResponse(actor.userId(), actor.workspaceId(), actor.tenantId(), actor.workspaceRole());
    }

    public record LoginRequest(@NotBlank String userId) {
    }

    public record LoginResponse(String token, String userId, RequestContextResponse requestContext) {
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
}
