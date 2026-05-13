package com.example.platform.identityaccess.domain;

import com.example.platform.common.domain.AbstractAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "workspace_memberships", indexes = {
        @Index(name = "idx_membership_workspace_user", columnList = "workspace_id,user_id", unique = true)
})
public class MembershipEntity extends AbstractAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Column(name = "workspace_id", nullable = false, length = 64)
    private String workspaceId;

    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    @Column(name = "role", nullable = false, length = 32)
    private String role;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    protected MembershipEntity() {
    }

    public MembershipEntity(String tenantId, String workspaceId, String userId, String role, String status) {
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

    public String getRole() {
        return role;
    }

    public String getStatus() {
        return status;
    }
}
