package com.example.platform.tasks.domain;

import com.example.platform.common.domain.AbstractAuditableEntity;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("tasks")
public class TaskEntity extends AbstractAuditableEntity {

    @Id
    @Column("task_id")
    private String taskId;

    @Column("tenant_id")
    private String tenantId;

    @Column("workspace_id")
    private String workspaceId;

    @Column("title")
    private String title;

    @Column("description")
    private String description;

    @Column("status")
    private TaskStatus status;

    @Column("assignee_user_id")
    private String assigneeUserId;

    @Column("created_by_user_id")
    private String createdByUserId;

    @Column("last_modified_by_user_id")
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

    @Override
    public Object getId() {
        return taskId;
    }
}
