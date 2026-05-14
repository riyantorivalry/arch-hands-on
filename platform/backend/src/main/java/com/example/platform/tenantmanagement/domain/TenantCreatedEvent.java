package com.example.platform.tenantmanagement.domain;

import com.example.platform.common.domain.DomainEvent;

/**
 * Event published when a new tenant is created.
 * Other modules can listen to bootstrap capabilities for this tenant.
 */
public class TenantCreatedEvent extends DomainEvent {
    private final String tenantName;
    private final String workspaceId;
    private final String workspaceName;

    public TenantCreatedEvent(String tenantId, String tenantName, String workspaceId, String workspaceName) {
        super(tenantId, tenantId);
        this.tenantName = tenantName;
        this.workspaceId = workspaceId;
        this.workspaceName = workspaceName;
    }

    @Override
    public String getEventType() {
        return "TenantCreatedEvent";
    }

    public String getTenantName() {
        return tenantName;
    }

    public String getWorkspaceId() {
        return workspaceId;
    }

    public String getWorkspaceName() {
        return workspaceName;
    }
}
