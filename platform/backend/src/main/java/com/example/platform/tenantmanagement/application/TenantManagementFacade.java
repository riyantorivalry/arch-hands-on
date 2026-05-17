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

    @Transactional
    public TenantView createTenant(String tenantName, String workspaceName, String ownerUserId, String ownerEmail, String ownerDisplayName) {
        String tenantId = "tenant-" + slugify(tenantName);
        String workspaceId = "workspace-" + slugify(workspaceName);

        if (tenantRepository.existsById(tenantId)) {
            throw new IllegalStateException("Tenant already exists: " + tenantId);
        }

        tenantRepository.save(new TenantEntity(tenantId, tenantName, TenantStatus.ACTIVE, "STANDARD"));
        workspaceRepository.save(new WorkspaceEntity(workspaceId, tenantId, workspaceName, WorkspaceStatus.ACTIVE));
        workspaceSettingsRepository.save(defaultSettings(workspaceId, tenantId));

        UserEntity user = userRepository.findById(ownerUserId)
                .orElseGet(() -> userRepository.save(new UserEntity(ownerUserId, ownerEmail, ownerDisplayName, UserStatus.ACTIVE)));
        membershipRepository.findByWorkspaceIdAndUserId(workspaceId, user.getUserId())
                .orElseGet(() -> membershipRepository.save(
                        new MembershipEntity(tenantId, workspaceId, user.getUserId(), MembershipRole.OWNER, MembershipStatus.ACTIVE)
                ));

        // Publish domain event
        domainEventPublisher.publish(new TenantCreatedEvent(tenantId, tenantName, workspaceId, workspaceName));

        return new TenantView(tenantId, workspaceId, tenantName, workspaceName, TenantStatus.ACTIVE.name());
    }

    @Transactional
    public WorkspaceView createWorkspace(String tenantId, String actorWorkspaceId, String actorUserId, String workspaceName) {
        tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + tenantId));

        MembershipEntity actorMembership = requireActiveMembership(actorWorkspaceId, actorUserId);
        if (!tenantId.equals(actorMembership.getTenantId())) {
            throw new AuthorizationDeniedException("Actor cannot create workspaces for another tenant");
        }
        authorizationService.requireWorkspaceManager(actorMembership);

        String workspaceId = "workspace-" + slugify(workspaceName);
        if (workspaceRepository.existsById(workspaceId)) {
            throw new IllegalStateException("Workspace already exists: " + workspaceId);
        }

        WorkspaceEntity workspace = workspaceRepository.save(new WorkspaceEntity(
                workspaceId,
                tenantId,
                workspaceName,
                WorkspaceStatus.ACTIVE
        ));
        workspaceSettingsRepository.save(defaultSettings(workspaceId, tenantId));

        membershipRepository.save(new MembershipEntity(
                tenantId,
                workspaceId,
                actorUserId,
                MembershipRole.OWNER,
                MembershipStatus.ACTIVE
        ));

        return toWorkspaceView(workspace);
    }

    @Transactional(readOnly = true)
    public WorkspaceView getWorkspace(String workspaceId, String actorUserId) {
        requireActiveMembership(workspaceId, actorUserId);
        var workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found: " + workspaceId));
        return toWorkspaceView(workspace);
    }

    @Transactional
    public WorkspaceSettingsView updateWorkspaceSettings(
            String workspaceId,
            String actorUserId,
            String defaultDocumentStatus,
            Boolean taskAutoAssignEnabled,
            Integer messageRetentionDays
    ) {
        MembershipEntity membership = requireActiveMembership(workspaceId, actorUserId);
        authorizationService.requireWorkspaceManager(membership);

        WorkspaceSettingsEntity settings = workspaceSettingsRepository.findByWorkspaceIdAndTenantId(workspaceId, membership.getTenantId())
                .orElseGet(() -> workspaceSettingsRepository.save(defaultSettings(workspaceId, membership.getTenantId())));
        settings.update(
                normalizeDefaultDocumentStatus(defaultDocumentStatus, settings.getDefaultDocumentStatus()),
                taskAutoAssignEnabled == null ? settings.isTaskAutoAssignEnabled() : taskAutoAssignEnabled,
                normalizeRetention(messageRetentionDays, settings.getMessageRetentionDays())
        );
        return toWorkspaceSettingsView(settings);
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

    private MembershipEntity requireActiveMembership(String workspaceId, String userId) {
        var membership = membershipRepository.findByWorkspaceIdAndUserId(workspaceId, userId)
                .orElseThrow(() -> new AuthorizationDeniedException("User is not a member of workspace " + workspaceId));
        if (membership.getStatus() != MembershipStatus.ACTIVE) {
            throw new IllegalStateException("Membership is not active for user " + userId);
        }
        return membership;
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
