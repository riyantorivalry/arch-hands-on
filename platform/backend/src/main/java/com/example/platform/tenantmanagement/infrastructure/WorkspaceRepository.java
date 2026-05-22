package com.example.platform.tenantmanagement.infrastructure;

import com.example.platform.tenantmanagement.domain.WorkspaceEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface WorkspaceRepository extends ReactiveCrudRepository<WorkspaceEntity, String> {

    Mono<WorkspaceEntity> findByWorkspaceIdAndTenantId(String workspaceId, String tenantId);
}
