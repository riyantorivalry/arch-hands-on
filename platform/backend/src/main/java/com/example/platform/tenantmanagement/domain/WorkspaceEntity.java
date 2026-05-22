package com.example.platform.tenantmanagement.domain;

import com.example.platform.common.domain.AbstractAuditableEntity;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("workspaces")
public class WorkspaceEntity extends AbstractAuditableEntity {

    @Id
    @Column("workspace_id")
    private String workspaceId;

    @Column("tenant_id")
    private String tenantId;

    @Column("name")
    private String name;

    @Column("status")
    private WorkspaceStatus status;

    protected WorkspaceEntity() {
    }

    public WorkspaceEntity(String workspaceId, String tenantId, String name, WorkspaceStatus status) {
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

    public WorkspaceStatus getStatus() {
        return status;
    }

    @Override
    public Object getId() {
        return workspaceId;
    }
}
