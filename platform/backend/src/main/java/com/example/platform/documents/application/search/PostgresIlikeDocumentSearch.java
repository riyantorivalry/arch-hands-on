package com.example.platform.documents.application.search;

import com.example.platform.documents.domain.DocumentEntity;
import com.example.platform.documents.infrastructure.DocumentRepository;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class PostgresIlikeDocumentSearch implements DocumentSearchUseCase {

    private final DocumentRepository documentRepository;

    public PostgresIlikeDocumentSearch(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    @Override
    public String version() {
        return "v1";
    }

    @Override
    public String implementation() {
        return "postgres-ilike";
    }

    @Override
    public Mono<List<DocumentSearchResult>> search(String workspaceId, String query, Pageable pageable) {
        return documentRepository.searchByWorkspaceIdAndQuery(
                        workspaceId,
                        query,
                        pageable.getPageSize(),
                        pageable.getOffset()
                )
                .map(this::toResult)
                .collectList();
    }

    private DocumentSearchResult toResult(DocumentEntity document) {
        return new DocumentSearchResult(
                document.getDocumentId(),
                document.getWorkspaceId(),
                document.getTitle(),
                document.getContent(),
                document.getStatus().name(),
                document.getCreatedByUserId(),
                document.getLastModifiedByUserId()
        );
    }
}
