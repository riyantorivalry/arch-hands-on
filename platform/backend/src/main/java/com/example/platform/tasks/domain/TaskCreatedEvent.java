package com.example.platform.tasks.domain;

import com.example.platform.common.domain.DomainEvent;

/**
 * Event published when a task is created.
 */
public class TaskCreatedEvent extends DomainEvent {
    private final String workspaceId;
    private final String title;
    private final String createdByUserId;

    public TaskCreatedEvent(String tenantId, String taskId, String workspaceId, String title, String createdByUserId) {
        super(tenantId, taskId);
        this.workspaceId = workspaceId;
        this.title = title;
        this.createdByUserId = createdByUserId;
    }

    @Override
    public String getEventType() {
        return "TaskCreatedEvent";
    }

    public String getWorkspaceId() {
        return workspaceId;
    }

    public String getTitle() {
        return title;
    }

    public String getCreatedByUserId() {
        return createdByUserId;
    }
}
