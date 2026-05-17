package com.example.platform.tenantmanagement.infrastructure;

import com.example.platform.tenantmanagement.domain.WorkspaceSettingsEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkspaceSettingsRepository extends JpaRepository<WorkspaceSettingsEntity, String> {

    Optional<WorkspaceSettingsEntity> findByWorkspaceIdAndTenantId(String workspaceId, String tenantId);
}
