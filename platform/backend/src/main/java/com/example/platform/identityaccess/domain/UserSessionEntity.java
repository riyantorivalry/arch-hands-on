package com.example.platform.identityaccess.domain;

import com.example.platform.common.domain.AbstractAuditableEntity;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import java.time.Instant;

@Table("user_sessions")
public class UserSessionEntity extends AbstractAuditableEntity {

    @Id
    @Column("session_token")
    private String sessionToken;

    @Column("tenant_id")
    private String tenantId;

    @Column("workspace_id")
    private String workspaceId;

    @Column("user_id")
    private String userId;

    @Column("status")
    private SessionStatus status;

    @Column("expires_at")
    private Instant expiresAt;

    protected UserSessionEntity() {
    }

    public UserSessionEntity(
            String sessionToken,
            String tenantId,
            String workspaceId,
            String userId,
            SessionStatus status,
            Instant expiresAt
    ) {
        this.sessionToken = sessionToken;
        this.tenantId = tenantId;
        this.workspaceId = workspaceId;
        this.userId = userId;
        this.status = status;
        this.expiresAt = expiresAt;
    }

    public String getSessionToken() {
        return sessionToken;
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

    public SessionStatus getStatus() {
        return status;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void revoke() {
        this.status = SessionStatus.REVOKED;
    }

    @Override
    public Object getId() {
        return sessionToken;
    }
}
