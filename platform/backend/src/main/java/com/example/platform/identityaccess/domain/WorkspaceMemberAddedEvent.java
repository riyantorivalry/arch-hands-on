package com.example.platform.identityaccess.domain;

import com.example.platform.common.domain.DomainEvent;

/**
 * Event published when a workspace membership is created or updated.
 */
public class WorkspaceMemberAddedEvent extends DomainEvent {
    private final String workspaceId;
    private final String userId;
    private final String role;

    public WorkspaceMemberAddedEvent(String tenantId, String workspaceId, String userId, String role) {
        super(tenantId, workspaceId);
        this.workspaceId = workspaceId;
        this.userId = userId;
        this.role = role;
    }

    @Override
    public String getEventType() {
        return "WorkspaceMemberAddedEvent";
    }

    public String getWorkspaceId() {
        return workspaceId;
    }

    public String getUserId() {
        return userId;
    }

    public String getRole() {
        return role;
    }
}
