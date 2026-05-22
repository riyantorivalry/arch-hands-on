package com.example.platform.messaging.infrastructure;

import com.example.platform.messaging.domain.ChannelEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ChannelRepository extends ReactiveCrudRepository<ChannelEntity, String> {

    Flux<ChannelEntity> findByWorkspaceIdOrderByNameAsc(String workspaceId);

    @Query("""
            select *
            from channels
            where workspace_id = :workspaceId
            order by name asc
            limit :limit
            offset :offset
            """)
    Flux<ChannelEntity> findByWorkspaceIdOrderByNameAsc(
            @Param("workspaceId") String workspaceId,
            @Param("limit") int limit,
            @Param("offset") long offset
    );

    Mono<ChannelEntity> findByChannelIdAndWorkspaceId(String channelId, String workspaceId);
}
