package com.example.platform.tenantmanagement.domain;

import com.example.platform.common.domain.AbstractAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "workspace_settings", indexes = {
        @Index(name = "idx_workspace_settings_tenant", columnList = "tenant_id")
})
public class WorkspaceSettingsEntity extends AbstractAuditableEntity {

    @Id
    @Column(name = "workspace_id", nullable = false, length = 64)
    private String workspaceId;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Column(name = "default_document_status", nullable = false, length = 32)
    private String defaultDocumentStatus;

    @Column(name = "task_auto_assign_enabled", nullable = false)
    private boolean taskAutoAssignEnabled;

    @Column(name = "message_retention_days", nullable = false)
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
}
