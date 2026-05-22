package com.example.platform.tenantmanagement.api;

import com.example.platform.common.web.RequestContexts;
import com.example.platform.tenantmanagement.application.TenantManagementFacade;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Validated
@RestController
@RequestMapping("/api")
public class TenantManagementController {

    private final TenantManagementFacade facade;

    public TenantManagementController(TenantManagementFacade facade) {
        this.facade = facade;
    }

    @PostMapping("/tenants")
    public Mono<TenantManagementFacade.TenantView> createTenant(@Valid @RequestBody CreateTenantRequest request) {
        return facade.createTenant(
                request.tenantName(),
                request.workspaceName(),
                request.ownerUserId(),
                request.ownerEmail(),
                request.ownerDisplayName()
        );
    }

    @PostMapping("/tenants/{tenantId}/workspaces")
    public Mono<TenantManagementFacade.WorkspaceView> createWorkspace(
            @PathVariable String tenantId,
            @Valid @RequestBody CreateWorkspaceRequest request
    ) {
        return RequestContexts.authenticatedReactive()
                .flatMap(context -> facade.createWorkspace(tenantId, context.workspaceId(), context.userId(), request.workspaceName()));
    }

    @GetMapping("/workspaces/{workspaceId}")
    public Mono<TenantManagementFacade.WorkspaceView> getWorkspace(@PathVariable String workspaceId) {
        return RequestContexts.authenticatedReactive()
                .flatMap(context -> facade.getWorkspace(workspaceId, context.userId()));
    }

    @PatchMapping("/workspaces/{workspaceId}/settings")
    public Mono<TenantManagementFacade.WorkspaceSettingsView> updateWorkspaceSettings(
            @PathVariable String workspaceId,
            @Valid @RequestBody UpdateWorkspaceSettingsRequest request
    ) {
        return RequestContexts.authenticatedReactive()
                .flatMap(context -> facade.updateWorkspaceSettings(
                        workspaceId,
                        context.userId(),
                        request.defaultDocumentStatus(),
                        request.taskAutoAssignEnabled(),
                        request.messageRetentionDays()
                ));
    }

    public record CreateTenantRequest(
            @NotBlank String tenantName,
            @NotBlank String workspaceName,
            @NotBlank String ownerUserId,
            @NotBlank String ownerEmail,
            @NotBlank String ownerDisplayName
    ) {
    }

    public record CreateWorkspaceRequest(@NotBlank String workspaceName) {
    }

    public record UpdateWorkspaceSettingsRequest(
            String defaultDocumentStatus,
            Boolean taskAutoAssignEnabled,
            @Min(1) @Max(3650) Integer messageRetentionDays
    ) {
    }
}
