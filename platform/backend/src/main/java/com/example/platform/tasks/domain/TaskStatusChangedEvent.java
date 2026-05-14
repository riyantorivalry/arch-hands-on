package com.example.platform.tasks.domain;

import com.example.platform.common.domain.DomainEvent;

/**
 * Event published when a task's status transitions.
 */
public class TaskStatusChangedEvent extends DomainEvent {
    private final String workspaceId;
    private final String previousStatus;
    private final String newStatus;

    public TaskStatusChangedEvent(String tenantId, String taskId, String workspaceId, String previousStatus, String newStatus) {
        super(tenantId, taskId);
        this.workspaceId = workspaceId;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
    }

    @Override
    public String getEventType() {
        return "TaskStatusChangedEvent";
    }

    public String getWorkspaceId() {
        return workspaceId;
    }

    public String getPreviousStatus() {
        return previousStatus;
    }

    public String getNewStatus() {
        return newStatus;
    }
}
