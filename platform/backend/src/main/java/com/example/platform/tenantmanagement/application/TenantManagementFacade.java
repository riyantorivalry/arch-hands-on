package com.example.platform.tenantmanagement.application;

import org.springframework.stereotype.Service;

@Service
public class TenantManagementFacade {

    public TenantView createTenant(String tenantName, String workspaceName) {
        return new TenantView("tenant-" + tenantName.toLowerCase().replace(" ", "-"),
                "workspace-" + workspaceName.toLowerCase().replace(" ", "-"),
                tenantName,
                workspaceName,
                "ACTIVE");
    }

    public WorkspaceView getWorkspace(String workspaceId) {
        return new WorkspaceView(workspaceId, "tenant-dev", "Engineering", "ACTIVE");
    }

    public WorkspaceView updateWorkspaceSettings(String workspaceId) {
        return new WorkspaceView(workspaceId, "tenant-dev", "Engineering", "ACTIVE");
    }

    public record TenantView(String tenantId, String workspaceId, String tenantName, String workspaceName, String status) {
    }

    public record WorkspaceView(String workspaceId, String tenantId, String workspaceName, String status) {
    }
}
