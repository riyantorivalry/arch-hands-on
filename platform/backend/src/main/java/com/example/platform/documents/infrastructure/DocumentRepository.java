package com.example.platform.documents.infrastructure;

import com.example.platform.documents.domain.DocumentEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DocumentRepository extends JpaRepository<DocumentEntity, String> {

    List<DocumentEntity> findByWorkspaceIdOrderByUpdatedAtDesc(String workspaceId);

    @Query("""
            select document
            from DocumentEntity document
            where document.workspaceId = :workspaceId
              and (
                lower(document.title) like lower(concat('%', :query, '%'))
                or lower(document.content) like lower(concat('%', :query, '%'))
              )
            order by document.updatedAt desc
            """)
    List<DocumentEntity> searchByWorkspaceIdAndQuery(
            @Param("workspaceId") String workspaceId,
            @Param("query") String query
    );
}
