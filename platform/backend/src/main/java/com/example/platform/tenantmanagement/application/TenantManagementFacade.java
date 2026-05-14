package com.example.platform.tenantmanagement.application;

import com.example.platform.common.domain.DomainEventPublisher;
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
import com.example.platform.tenantmanagement.domain.WorkspaceStatus;
import com.example.platform.tenantmanagement.infrastructure.TenantRepository;
import com.example.platform.tenantmanagement.infrastructure.WorkspaceRepository;
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

    public TenantManagementFacade(
            TenantRepository tenantRepository,
            WorkspaceRepository workspaceRepository,
            UserRepository userRepository,
            MembershipRepository membershipRepository,
            DomainEventPublisher domainEventPublisher
    ) {
        this.tenantRepository = tenantRepository;
        this.workspaceRepository = workspaceRepository;
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
        this.domainEventPublisher = domainEventPublisher;
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

    public WorkspaceView getWorkspace(String workspaceId) {
        var workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found: " + workspaceId));
        return new WorkspaceView(workspace.getWorkspaceId(), workspace.getTenantId(), workspace.getName(), workspace.getStatus().name());
    }

    public WorkspaceView updateWorkspaceSettings(String workspaceId) {
        return getWorkspace(workspaceId);
    }

    public record TenantView(String tenantId, String workspaceId, String tenantName, String workspaceName, String status) {
    }

    public record WorkspaceView(String workspaceId, String tenantId, String workspaceName, String status) {
    }

    private String slugify(String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return normalized.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }
}
