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
import reactor.core.publisher.Mono;

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

    @Transactional(transactionManager = "connectionFactoryTransactionManager")
    public Mono<AuthenticatedSession> login(String userId, String workspaceId) {
        return userRepository.findById(userId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("User not found: " + userId)))
                .then(membershipRepository.findByWorkspaceIdAndUserId(workspaceId, userId)
                        .switchIfEmpty(Mono.error(new IllegalArgumentException("Membership not found for workspace " + workspaceId))))
                .flatMap(membership -> {
                    if (membership.getStatus() != MembershipStatus.ACTIVE) {
                        return Mono.error(new IllegalStateException("Membership is not active for user " + userId));
                    }
                    String token = UUID.randomUUID().toString() + UUID.randomUUID().toString().replace("-", "");
                    Instant expiresAt = Instant.now().plus(SESSION_TTL);
                    return userSessionRepository.save(new UserSessionEntity(
                                    token,
                                    membership.getTenantId(),
                                    membership.getWorkspaceId(),
                                    membership.getUserId(),
                                    SessionStatus.ACTIVE,
                                    expiresAt
                            ))
                            .thenReturn(new AuthenticatedSession(token, membership.getTenantId(), membership.getWorkspaceId(), membership.getUserId(), expiresAt));
                });
    }

    @Transactional(transactionManager = "connectionFactoryTransactionManager", readOnly = true)
    public Mono<RequestContext> authenticate(String token, String correlationId) {
        return userSessionRepository.findBySessionToken(token)
                .switchIfEmpty(Mono.error(new AuthenticationRequiredException("Invalid session token")))
                .flatMap(session -> {
                    if (session.getStatus() != SessionStatus.ACTIVE || session.getExpiresAt().isBefore(Instant.now())) {
                        return Mono.error(new AuthenticationRequiredException("Session is expired or revoked"));
                    }
                    return Mono.just(new RequestContext(session.getTenantId(), session.getWorkspaceId(), session.getUserId(), correlationId));
                });
    }

    @Transactional(transactionManager = "connectionFactoryTransactionManager")
    public Mono<Void> logout(String token) {
        if (token == null || token.isBlank()) {
            return Mono.error(new AuthenticationRequiredException("Session token is required"));
        }
        return userSessionRepository.findBySessionToken(token)
                .switchIfEmpty(Mono.error(new AuthenticationRequiredException("Invalid session token")))
                .flatMap(session -> {
                    session.revoke();
                    return userSessionRepository.save(session);
                })
                .then();
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
