package com.example.platform.documents.infrastructure;

import com.example.platform.documents.domain.DocumentEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface DocumentRepository extends ReactiveCrudRepository<DocumentEntity, String> {

    Flux<DocumentEntity> findByWorkspaceIdOrderByUpdatedAtDesc(String workspaceId);

    @Query("""
            select *
            from documents
            where workspace_id = :workspaceId
            order by updated_at desc
            limit :limit
            offset :offset
            """)
    Flux<DocumentEntity> findByWorkspaceIdOrderByUpdatedAtDesc(
            @Param("workspaceId") String workspaceId,
            @Param("limit") int limit,
            @Param("offset") long offset
    );

    @Query("""
            select *
            from documents
            where workspace_id = :workspaceId
              and (
                lower(title) like lower(concat('%', :query, '%'))
                or lower(content) like lower(concat('%', :query, '%'))
              )
            order by updated_at desc
            limit :limit
            offset :offset
            """)
    Flux<DocumentEntity> searchByWorkspaceIdAndQuery(
            @Param("workspaceId") String workspaceId,
            @Param("query") String query,
            @Param("limit") int limit,
            @Param("offset") long offset
    );
}
