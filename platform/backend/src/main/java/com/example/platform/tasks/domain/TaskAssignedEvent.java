package com.example.platform.tasks.domain;

import com.example.platform.common.domain.DomainEvent;

/**
 * Event published when a task is assigned to a user.
 */
public class TaskAssignedEvent extends DomainEvent {
    private final String workspaceId;
    private final String assigneeUserId;

    public TaskAssignedEvent(String tenantId, String taskId, String workspaceId, String assigneeUserId) {
        super(tenantId, taskId);
        this.workspaceId = workspaceId;
        this.assigneeUserId = assigneeUserId;
    }

    @Override
    public String getEventType() {
        return "TaskAssignedEvent";
    }

    public String getWorkspaceId() {
        return workspaceId;
    }

    public String getAssigneeUserId() {
        return assigneeUserId;
    }
}

