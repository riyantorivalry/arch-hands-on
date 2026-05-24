package com.example.platform.identityaccess.application;

import com.example.platform.common.web.AuthenticationRequiredException;
import com.example.platform.common.web.RequestContext;
import com.example.platform.identityaccess.domain.MembershipStatus;
import com.example.platform.identityaccess.domain.SessionStatus;
import com.example.platform.identityaccess.domain.UserSessionEntity;
import com.example.platform.identityaccess.infrastructure.MembershipRepository;
import com.example.platform.identityaccess.infrastructure.UserRepository;
import com.example.platform.identityaccess.infrastructure.UserSessionRepository;
import com.example.platform.identityaccess.infrastructure.security.JwtAccessTokenService;
import com.example.platform.identityaccess.infrastructure.security.JwtAuthenticationProperties;
import com.example.platform.identityaccess.infrastructure.security.RefreshTokenService;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SessionAuthenticationService {

    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;
    private final UserSessionRepository userSessionRepository;
    private final JwtAccessTokenService jwtAccessTokenService;
    private final RefreshTokenService refreshTokenService;
    private final JwtAuthenticationProperties jwtProperties;

    public SessionAuthenticationService(
            UserRepository userRepository,
            MembershipRepository membershipRepository,
            UserSessionRepository userSessionRepository,
            JwtAccessTokenService jwtAccessTokenService,
            RefreshTokenService refreshTokenService,
            JwtAuthenticationProperties jwtProperties
    ) {
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
        this.userSessionRepository = userSessionRepository;
        this.jwtAccessTokenService = jwtAccessTokenService;
        this.refreshTokenService = refreshTokenService;
        this.jwtProperties = jwtProperties;
    }

    @Transactional
    public AuthenticatedSession login(String userId, String workspaceId) {
        return login(userId, workspaceId, AuthenticationClient.web());
    }

    @Transactional
    public AuthenticatedSession login(String userId, String workspaceId, AuthenticationClient client) {
        userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        var membership = membershipRepository.findByWorkspaceIdAndUserId(workspaceId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Membership not found for workspace " + workspaceId));
        if (membership.getStatus() != MembershipStatus.ACTIVE) {
            throw new IllegalStateException("Membership is not active for user " + userId);
        }

        Instant issuedAt = Instant.now();
        String sessionId = UUID.randomUUID().toString();
        String refreshToken = refreshTokenService.generate();
        Instant refreshExpiresAt = issuedAt.plus(jwtProperties.getRefreshTokenTtl());
        UserSessionEntity session = userSessionRepository.save(new UserSessionEntity(
                sessionId,
                refreshTokenService.hash(refreshToken),
                membership.getTenantId(),
                membership.getWorkspaceId(),
                membership.getUserId(),
                SessionStatus.ACTIVE,
                refreshExpiresAt,
                client.clientId(),
                client.clientType().name(),
                issuedAt
        ));
        var accessToken = jwtAccessTokenService.issue(session, issuedAt);
        return new AuthenticatedSession(
                session.getSessionToken(),
                session.getTenantId(),
                session.getWorkspaceId(),
                session.getUserId(),
                session.getClientId(),
                session.getClientType(),
                new TokenPair(
                        accessToken.token(),
                        refreshToken,
                        "Bearer",
                        accessToken.expiresAt(),
                        refreshExpiresAt,
                        accessToken.expiresIn()
                )
        );
    }

    @Transactional
    public RequestContext authenticate(String accessToken, String correlationId) {
        var claims = jwtAccessTokenService.verify(accessToken);
        UserSessionEntity session = userSessionRepository.findBySessionToken(claims.sessionId())
                .orElseThrow(() -> new AuthenticationRequiredException("Invalid session"));
        Instant now = Instant.now();
        if (session.getStatus() != SessionStatus.ACTIVE || !session.getUserId().equals(claims.userId())
                || !session.getTenantId().equals(claims.tenantId()) || !session.getWorkspaceId().equals(claims.workspaceId())
                || session.getExpiresAt().isBefore(now)) {
            throw new AuthenticationRequiredException("Session is expired or revoked");
        }
        session.markSeen(now);
        return new RequestContext(session.getTenantId(), session.getWorkspaceId(), session.getUserId(), correlationId);
    }

    @Transactional
    public AuthenticatedSession refresh(String refreshToken) {
        String refreshTokenHash = refreshTokenService.hash(refreshToken);
        UserSessionEntity session = userSessionRepository.findByRefreshTokenHash(refreshTokenHash)
                .orElseThrow(() -> new AuthenticationRequiredException("Invalid refresh token"));
        Instant now = Instant.now();
        if (session.getStatus() != SessionStatus.ACTIVE || session.getExpiresAt().isBefore(now)) {
            throw new AuthenticationRequiredException("Session is expired or revoked");
        }

        String nextRefreshToken = refreshTokenService.generate();
        Instant nextRefreshExpiresAt = now.plus(jwtProperties.getRefreshTokenTtl());
        session.rotateRefreshToken(refreshTokenService.hash(nextRefreshToken), nextRefreshExpiresAt);
        var accessToken = jwtAccessTokenService.issue(session, now);
        return new AuthenticatedSession(
                session.getSessionToken(),
                session.getTenantId(),
                session.getWorkspaceId(),
                session.getUserId(),
                session.getClientId(),
                session.getClientType(),
                new TokenPair(
                        accessToken.token(),
                        nextRefreshToken,
                        "Bearer",
                        accessToken.expiresAt(),
                        nextRefreshExpiresAt,
                        accessToken.expiresIn()
                )
        );
    }

    @Transactional
    public void logout(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new AuthenticationRequiredException("Access token is required");
        }
        var claims = jwtAccessTokenService.verify(accessToken);
        UserSessionEntity session = userSessionRepository.findBySessionToken(claims.sessionId())
                .orElseThrow(() -> new AuthenticationRequiredException("Invalid session"));
        session.revoke();
    }

    public record AuthenticatedSession(
            String sessionId,
            String tenantId,
            String workspaceId,
            String userId,
            String clientId,
            String clientType,
            TokenPair tokens
    ) {
    }
}
