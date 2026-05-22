package com.example.platform.tenantmanagement.infrastructure;

import com.example.platform.tenantmanagement.domain.WorkspaceSettingsEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface WorkspaceSettingsRepository extends ReactiveCrudRepository<WorkspaceSettingsEntity, String> {

    Mono<WorkspaceSettingsEntity> findByWorkspaceIdAndTenantId(String workspaceId, String tenantId);
}
