package com.example.platform.tenantmanagement.domain;

import com.example.platform.common.domain.AbstractAuditableEntity;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("workspace_settings")
public class WorkspaceSettingsEntity extends AbstractAuditableEntity {

    @Id
    @Column("workspace_id")
    private String workspaceId;

    @Column("tenant_id")
    private String tenantId;

    @Column("default_document_status")
    private String defaultDocumentStatus;

    @Column("task_auto_assign_enabled")
    private boolean taskAutoAssignEnabled;

    @Column("message_retention_days")
    private int messageRetentionDays;

    protected WorkspaceSettingsEntity() {
    }

    public WorkspaceSettingsEntity(
            String workspaceId,
            String tenantId,
            String defaultDocumentStatus,
            boolean taskAutoAssignEnabled,
            int messageRetentionDays
    ) {
        this.workspaceId = workspaceId;
        this.tenantId = tenantId;
        this.defaultDocumentStatus = defaultDocumentStatus;
        this.taskAutoAssignEnabled = taskAutoAssignEnabled;
        this.messageRetentionDays = messageRetentionDays;
    }

    public String getWorkspaceId() {
        return workspaceId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getDefaultDocumentStatus() {
        return defaultDocumentStatus;
    }

    public boolean isTaskAutoAssignEnabled() {
        return taskAutoAssignEnabled;
    }

    public int getMessageRetentionDays() {
        return messageRetentionDays;
    }

    public void update(String defaultDocumentStatus, boolean taskAutoAssignEnabled, int messageRetentionDays) {
        this.defaultDocumentStatus = defaultDocumentStatus;
        this.taskAutoAssignEnabled = taskAutoAssignEnabled;
        this.messageRetentionDays = messageRetentionDays;
    }

    @Override
    public Object getId() {
        return workspaceId;
    }
}
