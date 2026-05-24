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

    @Column(name = "refresh_token_hash", nullable = false, length = 128)
    private String refreshTokenHash;

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

    @Column(name = "client_id", nullable = false, length = 120)
    private String clientId;

    @Column(name = "client_type", nullable = false, length = 32)
    private String clientType;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    @Column(name = "last_seen_at")
    private Instant lastSeenAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    protected UserSessionEntity() {
    }

    public UserSessionEntity(
            String sessionToken,
            String refreshTokenHash,
            String tenantId,
            String workspaceId,
            String userId,
            SessionStatus status,
            Instant expiresAt,
            String clientId,
            String clientType,
            Instant issuedAt
    ) {
        this.sessionToken = sessionToken;
        this.refreshTokenHash = refreshTokenHash;
        this.tenantId = tenantId;
        this.workspaceId = workspaceId;
        this.userId = userId;
        this.status = status;
        this.expiresAt = expiresAt;
        this.clientId = clientId;
        this.clientType = clientType;
        this.issuedAt = issuedAt;
    }

    public String getSessionToken() {
        return sessionToken;
    }

    public String getRefreshTokenHash() {
        return refreshTokenHash;
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

    public String getClientId() {
        return clientId;
    }

    public String getClientType() {
        return clientType;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public Instant getLastSeenAt() {
        return lastSeenAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public void rotateRefreshToken(String refreshTokenHash, Instant expiresAt) {
        this.refreshTokenHash = refreshTokenHash;
        this.expiresAt = expiresAt;
        this.lastSeenAt = Instant.now();
    }

    public void markSeen(Instant seenAt) {
        this.lastSeenAt = seenAt;
    }

    public void revoke() {
        this.status = SessionStatus.REVOKED;
        this.revokedAt = Instant.now();
    }
}
