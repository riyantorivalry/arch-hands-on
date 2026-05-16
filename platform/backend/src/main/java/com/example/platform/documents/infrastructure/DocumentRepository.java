package com.example.platform.documents.infrastructure;

import com.example.platform.documents.domain.DocumentEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<DocumentEntity, String> {

    List<DocumentEntity> findByWorkspaceIdOrderByUpdatedAtDesc(String workspaceId);

    List<DocumentEntity> findByWorkspaceIdAndTitleContainingOrContentContainingOrderByUpdatedAtDesc(
            String workspaceId, String titleQuery, String contentQuery);
}
