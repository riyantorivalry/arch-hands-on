package com.example.platform.tenantmanagement.application;

import com.example.platform.identityaccess.domain.MembershipEntity;
import com.example.platform.identityaccess.domain.UserEntity;
import com.example.platform.identityaccess.infrastructure.MembershipRepository;
import com.example.platform.identityaccess.infrastructure.UserRepository;
import com.example.platform.tenantmanagement.domain.TenantEntity;
import com.example.platform.tenantmanagement.domain.WorkspaceEntity;
import com.example.platform.tenantmanagement.infrastructure.TenantRepository;
import com.example.platform.tenantmanagement.infrastructure.WorkspaceRepository;
import java.text.Normalizer;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

@Service
public class TenantManagementFacade {

    private final TenantRepository tenantRepository;
    private final WorkspaceRepository workspaceRepository;
    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;

    public TenantManagementFacade(
            TenantRepository tenantRepository,
            WorkspaceRepository workspaceRepository,
            UserRepository userRepository,
            MembershipRepository membershipRepository
    ) {
        this.tenantRepository = tenantRepository;
        this.workspaceRepository = workspaceRepository;
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
    }

    @Transactional
    public TenantView createTenant(String tenantName, String workspaceName, String ownerUserId, String ownerEmail, String ownerDisplayName) {
        String tenantId = "tenant-" + slugify(tenantName);
        String workspaceId = "workspace-" + slugify(workspaceName);

        if (tenantRepository.existsById(tenantId)) {
            throw new IllegalStateException("Tenant already exists: " + tenantId);
        }

        tenantRepository.save(new TenantEntity(tenantId, tenantName, "ACTIVE", "STANDARD"));
        workspaceRepository.save(new WorkspaceEntity(workspaceId, tenantId, workspaceName, "ACTIVE"));

        UserEntity user = userRepository.findById(ownerUserId)
                .orElseGet(() -> userRepository.save(new UserEntity(ownerUserId, ownerEmail, ownerDisplayName, "ACTIVE")));
        membershipRepository.findByWorkspaceIdAndUserId(workspaceId, user.getUserId())
                .orElseGet(() -> membershipRepository.save(
                        new MembershipEntity(tenantId, workspaceId, user.getUserId(), "OWNER", "ACTIVE")
                ));

        return new TenantView(tenantId, workspaceId, tenantName, workspaceName, "ACTIVE");
    }

    public WorkspaceView getWorkspace(String workspaceId) {
        var workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found: " + workspaceId));
        return new WorkspaceView(workspace.getWorkspaceId(), workspace.getTenantId(), workspace.getName(), workspace.getStatus());
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
