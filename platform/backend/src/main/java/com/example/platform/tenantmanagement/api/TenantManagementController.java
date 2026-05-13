package com.example.platform.tenantmanagement.api;

import com.example.platform.tenantmanagement.application.TenantManagementFacade;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api")
public class TenantManagementController {

    private final TenantManagementFacade facade;

    public TenantManagementController(TenantManagementFacade facade) {
        this.facade = facade;
    }

    @PostMapping("/tenants")
    public TenantManagementFacade.TenantView createTenant(@RequestBody CreateTenantRequest request) {
        return facade.createTenant(request.tenantName(), request.workspaceName());
    }

    @PostMapping("/tenants/{tenantId}/workspaces")
    public TenantManagementFacade.WorkspaceView createWorkspace(
            @PathVariable String tenantId,
            @RequestBody CreateWorkspaceRequest request
    ) {
        return new TenantManagementFacade.WorkspaceView(
                "workspace-" + request.workspaceName().toLowerCase().replace(" ", "-"),
                tenantId,
                request.workspaceName(),
                "ACTIVE"
        );
    }

    @GetMapping("/workspaces/{workspaceId}")
    public TenantManagementFacade.WorkspaceView getWorkspace(@PathVariable String workspaceId) {
        return facade.getWorkspace(workspaceId);
    }

    @PatchMapping("/workspaces/{workspaceId}/settings")
    public TenantManagementFacade.WorkspaceView updateWorkspaceSettings(@PathVariable String workspaceId) {
        return facade.updateWorkspaceSettings(workspaceId);
    }

    public record CreateTenantRequest(@NotBlank String tenantName, @NotBlank String workspaceName) {
    }

    public record CreateWorkspaceRequest(@NotBlank String workspaceName) {
    }
}
