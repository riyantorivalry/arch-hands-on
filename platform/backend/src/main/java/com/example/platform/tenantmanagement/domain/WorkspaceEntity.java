package com.example.platform.tenantmanagement.domain;

import com.example.platform.common.domain.AbstractAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "workspaces", indexes = {
        @Index(name = "idx_workspace_tenant", columnList = "tenant_id")
})
public class WorkspaceEntity extends AbstractAuditableEntity {

    @Id
    @Column(name = "workspace_id", nullable = false, length = 64)
    private String workspaceId;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    protected WorkspaceEntity() {
    }

    public WorkspaceEntity(String workspaceId, String tenantId, String name, String status) {
        this.workspaceId = workspaceId;
        this.tenantId = tenantId;
        this.name = name;
        this.status = status;
    }

    public String getWorkspaceId() {
        return workspaceId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getName() {
        return name;
    }

    public String getStatus() {
        return status;
    }
}
