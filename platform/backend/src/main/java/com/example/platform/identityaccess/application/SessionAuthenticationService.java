package com.example.platform.identityaccess.application;

import com.example.platform.common.web.AuthenticationRequiredException;
import com.example.platform.common.web.RequestContext;
import com.example.platform.identityaccess.domain.MembershipStatus;
import com.example.platform.identityaccess.domain.SessionStatus;
import com.example.platform.identityaccess.domain.UserSessionEntity;
import com.example.platform.identityaccess.infrastructure.MembershipRepository;
import com.example.platform.identityaccess.infrastructure.UserRepository;
import com.example.platform.identityaccess.infrastructure.UserSessionRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SessionAuthenticationService {

    private static final Duration SESSION_TTL = Duration.ofHours(12);

    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;
    private final UserSessionRepository userSessionRepository;

    public SessionAuthenticationService(
            UserRepository userRepository,
            MembershipRepository membershipRepository,
            UserSessionRepository userSessionRepository
    ) {
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
        this.userSessionRepository = userSessionRepository;
    }

    @Transactional
    public AuthenticatedSession login(String userId, String workspaceId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        var membership = membershipRepository.findByWorkspaceIdAndUserId(workspaceId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Membership not found for workspace " + workspaceId));
        if (membership.getStatus() != MembershipStatus.ACTIVE) {
            throw new IllegalStateException("Membership is not active for user " + userId);
        }

        String token = UUID.randomUUID().toString() + UUID.randomUUID().toString().replace("-", "");
        Instant expiresAt = Instant.now().plus(SESSION_TTL);
        userSessionRepository.save(new UserSessionEntity(
                token,
                membership.getTenantId(),
                membership.getWorkspaceId(),
                membership.getUserId(),
                SessionStatus.ACTIVE,
                expiresAt
        ));
        return new AuthenticatedSession(token, membership.getTenantId(), membership.getWorkspaceId(), membership.getUserId(), expiresAt);
    }

    @Transactional(readOnly = true)
    public RequestContext authenticate(String token, String correlationId) {
        UserSessionEntity session = userSessionRepository.findBySessionToken(token)
                .orElseThrow(() -> new AuthenticationRequiredException("Invalid session token"));
        if (session.getStatus() != SessionStatus.ACTIVE || session.getExpiresAt().isBefore(Instant.now())) {
            throw new AuthenticationRequiredException("Session is expired or revoked");
        }
        return new RequestContext(session.getTenantId(), session.getWorkspaceId(), session.getUserId(), correlationId);
    }

    @Transactional
    public void logout(String token) {
        if (token == null || token.isBlank()) {
            throw new AuthenticationRequiredException("Session token is required");
        }
        UserSessionEntity session = userSessionRepository.findBySessionToken(token)
                .orElseThrow(() -> new AuthenticationRequiredException("Invalid session token"));
        session.revoke();
    }

    public record AuthenticatedSession(
            String token,
            String tenantId,
            String workspaceId,
            String userId,
            Instant expiresAt
    ) {
    }
}
