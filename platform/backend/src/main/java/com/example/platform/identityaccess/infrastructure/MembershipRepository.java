package com.example.platform.identityaccess.infrastructure;

import com.example.platform.identityaccess.domain.MembershipEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface MembershipRepository extends ReactiveCrudRepository<MembershipEntity, Long> {

    Mono<MembershipEntity> findByWorkspaceIdAndUserId(String workspaceId, String userId);
}
