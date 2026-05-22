package com.example.platform.tenantmanagement.application;

import com.example.platform.common.domain.DomainEventPublisher;
import com.example.platform.common.web.AuthorizationDeniedException;
import com.example.platform.identityaccess.application.AuthorizationService;
import com.example.platform.identityaccess.domain.MembershipEntity;
import com.example.platform.identityaccess.domain.MembershipRole;
import com.example.platform.identityaccess.domain.MembershipStatus;
import com.example.platform.identityaccess.domain.UserEntity;
import com.example.platform.identityaccess.domain.UserStatus;
import com.example.platform.identityaccess.infrastructure.MembershipRepository;
import com.example.platform.identityaccess.infrastructure.UserRepository;
import com.example.platform.tenantmanagement.domain.TenantCreatedEvent;
import com.example.platform.tenantmanagement.domain.TenantEntity;
import com.example.platform.tenantmanagement.domain.TenantStatus;
import com.example.platform.tenantmanagement.domain.WorkspaceEntity;
import com.example.platform.tenantmanagement.domain.WorkspaceSettingsEntity;
import com.example.platform.tenantmanagement.domain.WorkspaceStatus;
import com.example.platform.tenantmanagement.infrastructure.TenantRepository;
import com.example.platform.tenantmanagement.infrastructure.WorkspaceRepository;
import com.example.platform.tenantmanagement.infrastructure.WorkspaceSettingsRepository;
import java.text.Normalizer;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

@Service
public class TenantManagementFacade {

    private final TenantRepository tenantRepository;
    private final WorkspaceRepository workspaceRepository;
    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;
    private final DomainEventPublisher domainEventPublisher;
    private final WorkspaceSettingsRepository workspaceSettingsRepository;
    private final AuthorizationService authorizationService;

    public TenantManagementFacade(
            TenantRepository tenantRepository,
            WorkspaceRepository workspaceRepository,
            UserRepository userRepository,
            MembershipRepository membershipRepository,
            DomainEventPublisher domainEventPublisher,
            WorkspaceSettingsRepository workspaceSettingsRepository,
            AuthorizationService authorizationService
    ) {
        this.tenantRepository = tenantRepository;
        this.workspaceRepository = workspaceRepository;
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
        this.domainEventPublisher = domainEventPublisher;
        this.workspaceSettingsRepository = workspaceSettingsRepository;
        this.authorizationService = authorizationService;
    }

    @Transactional(transactionManager = "connectionFactoryTransactionManager")
    public Mono<TenantView> createTenant(String tenantName, String workspaceName, String ownerUserId, String ownerEmail, String ownerDisplayName) {
        String tenantId = "tenant-" + slugify(tenantName);
        String workspaceId = "workspace-" + slugify(workspaceName);

        return tenantRepository.existsById(tenantId)
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new IllegalStateException("Tenant already exists: " + tenantId));
                    }
                    return tenantRepository.save(new TenantEntity(tenantId, tenantName, TenantStatus.ACTIVE, "STANDARD"))
                            .then(workspaceRepository.save(new WorkspaceEntity(workspaceId, tenantId, workspaceName, WorkspaceStatus.ACTIVE)))
                            .then(workspaceSettingsRepository.save(defaultSettings(workspaceId, tenantId)))
                            .then(userRepository.findById(ownerUserId)
                                    .switchIfEmpty(userRepository.save(new UserEntity(ownerUserId, ownerEmail, ownerDisplayName, UserStatus.ACTIVE))))
                            .flatMap(user -> membershipRepository.findByWorkspaceIdAndUserId(workspaceId, user.getUserId())
                                    .switchIfEmpty(membershipRepository.save(
                                            new MembershipEntity(tenantId, workspaceId, user.getUserId(), MembershipRole.OWNER, MembershipStatus.ACTIVE)
                                    )))
                            .flatMap(membership -> domainEventPublisher.publish(new TenantCreatedEvent(tenantId, tenantName, workspaceId, workspaceName)))
                            .thenReturn(new TenantView(tenantId, workspaceId, tenantName, workspaceName, TenantStatus.ACTIVE.name()));
                });
    }

    @Transactional(transactionManager = "connectionFactoryTransactionManager")
    public Mono<WorkspaceView> createWorkspace(String tenantId, String actorWorkspaceId, String actorUserId, String workspaceName) {
        String workspaceId = "workspace-" + slugify(workspaceName);
        return tenantRepository.findById(tenantId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Tenant not found: " + tenantId)))
                .then(requireActiveMembership(actorWorkspaceId, actorUserId))
                .flatMap(actorMembership -> {
                    if (!tenantId.equals(actorMembership.getTenantId())) {
                        return Mono.error(new AuthorizationDeniedException("Actor cannot create workspaces for another tenant"));
                    }
                    return authorizationService.requireWorkspaceManager(actorMembership)
                            .then(workspaceRepository.existsById(workspaceId))
                            .flatMap(exists -> {
                                if (exists) {
                                    return Mono.error(new IllegalStateException("Workspace already exists: " + workspaceId));
                                }
                                return workspaceRepository.save(new WorkspaceEntity(
                                                workspaceId,
                                                tenantId,
                                                workspaceName,
                                                WorkspaceStatus.ACTIVE
                                        ))
                                        .flatMap(workspace -> workspaceSettingsRepository.save(defaultSettings(workspaceId, tenantId))
                                                .then(membershipRepository.save(new MembershipEntity(
                                                        tenantId,
                                                        workspaceId,
                                                        actorUserId,
                                                        MembershipRole.OWNER,
                                                        MembershipStatus.ACTIVE
                                                )))
                                                .thenReturn(toWorkspaceView(workspace)));
                            });
                });
    }

    @Transactional(transactionManager = "connectionFactoryTransactionManager", readOnly = true)
    public Mono<WorkspaceView> getWorkspace(String workspaceId, String actorUserId) {
        return requireActiveMembership(workspaceId, actorUserId)
                .then(workspaceRepository.findById(workspaceId)
                        .switchIfEmpty(Mono.error(new IllegalArgumentException("Workspace not found: " + workspaceId))))
                .map(this::toWorkspaceView);
    }

    @Transactional(transactionManager = "connectionFactoryTransactionManager")
    public Mono<WorkspaceSettingsView> updateWorkspaceSettings(
            String workspaceId,
            String actorUserId,
            String defaultDocumentStatus,
            Boolean taskAutoAssignEnabled,
            Integer messageRetentionDays
    ) {
        return requireActiveMembership(workspaceId, actorUserId)
                .flatMap(membership -> {
                    return authorizationService.requireWorkspaceManager(membership)
                            .then(workspaceSettingsRepository.findByWorkspaceIdAndTenantId(workspaceId, membership.getTenantId()))
                            .switchIfEmpty(workspaceSettingsRepository.save(defaultSettings(workspaceId, membership.getTenantId())))
                            .flatMap(settings -> {
                                settings.update(
                                        normalizeDefaultDocumentStatus(defaultDocumentStatus, settings.getDefaultDocumentStatus()),
                                        taskAutoAssignEnabled == null ? settings.isTaskAutoAssignEnabled() : taskAutoAssignEnabled,
                                        normalizeRetention(messageRetentionDays, settings.getMessageRetentionDays())
                                );
                                return workspaceSettingsRepository.save(settings).map(this::toWorkspaceSettingsView);
                            });
                });
    }

    public record TenantView(String tenantId, String workspaceId, String tenantName, String workspaceName, String status) {
    }

    public record WorkspaceView(String workspaceId, String tenantId, String workspaceName, String status) {
    }

    public record WorkspaceSettingsView(
            String workspaceId,
            String tenantId,
            String defaultDocumentStatus,
            boolean taskAutoAssignEnabled,
            int messageRetentionDays
    ) {
    }

    private Mono<MembershipEntity> requireActiveMembership(String workspaceId, String userId) {
        return membershipRepository.findByWorkspaceIdAndUserId(workspaceId, userId)
                .switchIfEmpty(Mono.error(new AuthorizationDeniedException("User is not a member of workspace " + workspaceId)))
                .flatMap(membership -> {
                    if (membership.getStatus() != MembershipStatus.ACTIVE) {
                        return Mono.error(new IllegalStateException("Membership is not active for user " + userId));
                    }
                    return Mono.just(membership);
                });
    }

    private WorkspaceSettingsEntity defaultSettings(String workspaceId, String tenantId) {
        return new WorkspaceSettingsEntity(workspaceId, tenantId, "DRAFT", true, 365);
    }

    private WorkspaceView toWorkspaceView(WorkspaceEntity workspace) {
        return new WorkspaceView(workspace.getWorkspaceId(), workspace.getTenantId(), workspace.getName(), workspace.getStatus().name());
    }

    private WorkspaceSettingsView toWorkspaceSettingsView(WorkspaceSettingsEntity settings) {
        return new WorkspaceSettingsView(
                settings.getWorkspaceId(),
                settings.getTenantId(),
                settings.getDefaultDocumentStatus(),
                settings.isTaskAutoAssignEnabled(),
                settings.getMessageRetentionDays()
        );
    }

    private String normalizeDefaultDocumentStatus(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (!normalized.equals("DRAFT") && !normalized.equals("IN_REVIEW")) {
            throw new IllegalArgumentException("Default document status must be DRAFT or IN_REVIEW");
        }
        return normalized;
    }

    private int normalizeRetention(Integer value, int fallback) {
        if (value == null) {
            return fallback;
        }
        if (value < 1 || value > 3650) {
            throw new IllegalArgumentException("Message retention days must be between 1 and 3650");
        }
        return value;
    }

    private String slugify(String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return normalized.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }
}
