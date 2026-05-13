package com.example.platform.identityaccess.domain;

import com.example.platform.common.domain.AbstractAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "user_sessions")
public class UserSessionEntity extends AbstractAuditableEntity {

    @Id
    @Column(name = "session_token", nullable = false, length = 128)
    private String sessionToken;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Column(name = "workspace_id", nullable = false, length = 64)
    private String workspaceId;

    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private SessionStatus status;

    @Column(name = "expires_at", nullable = false)
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
}
