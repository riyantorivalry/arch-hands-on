package com.example.platform.tenantmanagement.infrastructure;

import com.example.platform.tenantmanagement.domain.WorkspaceEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkspaceRepository extends JpaRepository<WorkspaceEntity, String> {

    Optional<WorkspaceEntity> findByWorkspaceIdAndTenantId(String workspaceId, String tenantId);
}
