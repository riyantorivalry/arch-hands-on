package com.example.platform.documents.infrastructure;

import com.example.platform.documents.domain.DocumentSearchDocument;
import java.util.List;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface DocumentSearchRepository extends ElasticsearchRepository<DocumentSearchDocument, String> {

    /**
     * Search for documents by title or content using full-text search in a workspace.
     */
    List<DocumentSearchDocument> findByWorkspaceIdAndTitleContainsOrContentContains(
            String workspaceId, String titleQuery, String contentQuery);

    /**
     * Find all documents in a workspace for indexing.
     */
    List<DocumentSearchDocument> findByWorkspaceId(String workspaceId);

    /**
     * Find all documents by tenant for maintenance/reindexing.
     */
    List<DocumentSearchDocument> findByTenantId(String tenantId);
}

