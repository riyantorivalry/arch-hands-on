package com.example.platform.tasks.infrastructure;

import com.example.platform.tasks.domain.TaskEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface TaskRepository extends ReactiveCrudRepository<TaskEntity, String> {

    Flux<TaskEntity> findByWorkspaceIdOrderByUpdatedAtDesc(String workspaceId);

    @Query("""
            select *
            from tasks
            where workspace_id = :workspaceId
            order by updated_at desc
            limit :limit
            offset :offset
            """)
    Flux<TaskEntity> findByWorkspaceIdOrderByUpdatedAtDesc(
            @Param("workspaceId") String workspaceId,
            @Param("limit") int limit,
            @Param("offset") long offset
    );
}
