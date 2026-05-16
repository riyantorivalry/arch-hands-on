package com.example.platform.tasks.domain;

import com.example.platform.common.domain.AbstractAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "tasks", indexes = {
        @Index(name = "idx_tasks_workspace", columnList = "workspace_id,updated_at")
})
public class TaskEntity extends AbstractAuditableEntity {

    @Id
    @Column(name = "task_id", nullable = false, length = 64)
    private String taskId;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Column(name = "workspace_id", nullable = false, length = 64)
    private String workspaceId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", nullable = false, length = 4000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private TaskStatus status;

    @Column(name = "assignee_user_id", length = 64)
    private String assigneeUserId;

    @Column(name = "created_by_user_id", nullable = false, length = 64)
    private String createdByUserId;

    @Column(name = "last_modified_by_user_id", nullable = false, length = 64)
    private String lastModifiedByUserId;

    protected TaskEntity() {
    }

    public TaskEntity(
            String taskId,
            String tenantId,
            String workspaceId,
            String title,
            String description,
            TaskStatus status,
            String assigneeUserId,
            String createdByUserId,
            String lastModifiedByUserId
    ) {
        this.taskId = taskId;
        this.tenantId = tenantId;
        this.workspaceId = workspaceId;
        this.title = title;
        this.description = description;
        this.status = status;
        this.assigneeUserId = assigneeUserId;
        this.createdByUserId = createdByUserId;
        this.lastModifiedByUserId = lastModifiedByUserId;
    }

    public String getTaskId() {
        return taskId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getWorkspaceId() {
        return workspaceId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public String getAssigneeUserId() {
        return assigneeUserId;
    }

    public String getCreatedByUserId() {
        return createdByUserId;
    }

    public void update(String title, String description, TaskStatus status, String assigneeUserId, String modifiedByUserId) {
        this.title = title;
        this.description = description;
        this.status = status;
        this.assigneeUserId = assigneeUserId;
        this.lastModifiedByUserId = modifiedByUserId;
    }
}
