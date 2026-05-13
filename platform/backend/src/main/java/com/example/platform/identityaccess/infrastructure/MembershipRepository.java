package com.example.platform.identityaccess.infrastructure;

import com.example.platform.identityaccess.domain.MembershipEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MembershipRepository extends JpaRepository<MembershipEntity, Long> {

    Optional<MembershipEntity> findByWorkspaceIdAndUserId(String workspaceId, String userId);
}
