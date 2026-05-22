package com.example.platform.identityaccess.domain;

import com.example.platform.common.domain.AbstractAuditableEntity;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("workspace_memberships")
public class MembershipEntity extends AbstractAuditableEntity {

    @Id
    private Long id;

    @Column("tenant_id")
    private String tenantId;

    @Column("workspace_id")
    private String workspaceId;

    @Column("user_id")
    private String userId;

    @Column("role")
    private MembershipRole role;

    @Column("status")
    private MembershipStatus status;

    protected MembershipEntity() {
    }

    public MembershipEntity(String tenantId, String workspaceId, String userId, MembershipRole role, MembershipStatus status) {
        this.tenantId = tenantId;
        this.workspaceId = workspaceId;
        this.userId = userId;
        this.role = role;
        this.status = status;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getWorkspaceId() {
        return workspaceId;
    }

    public String getUserId() {
        return userId;
    }

    public MembershipRole getRole() {
        return role;
    }

    public MembershipStatus getStatus() {
        return status;
    }

    @Override
    public Object getId() {
        return id;
    }
}
