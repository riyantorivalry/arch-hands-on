package com.example.platform.messaging.infrastructure;

import com.example.platform.messaging.domain.ChannelEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChannelRepository extends JpaRepository<ChannelEntity, String> {

    List<ChannelEntity> findByWorkspaceIdOrderByNameAsc(String workspaceId);

    List<ChannelEntity> findByWorkspaceIdOrderByNameAsc(String workspaceId, Pageable pageable);

    Optional<ChannelEntity> findByChannelIdAndWorkspaceId(String channelId, String workspaceId);
}
