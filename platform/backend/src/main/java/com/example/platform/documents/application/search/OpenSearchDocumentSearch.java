package com.example.platform.documents.application.search;

import com.example.platform.documents.infrastructure.DocumentSearchRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class OpenSearchDocumentSearch implements DocumentSearchUseCase {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenSearchDocumentSearch.class);

    private final DocumentSearchRepository documentSearchRepository;
    private final PostgresIlikeDocumentSearch fallbackSearch;

    public OpenSearchDocumentSearch(
            DocumentSearchRepository documentSearchRepository,
            PostgresIlikeDocumentSearch fallbackSearch
    ) {
        this.documentSearchRepository = documentSearchRepository;
        this.fallbackSearch = fallbackSearch;
    }

    @Override
    public String version() {
        return "v3";
    }

    @Override
    public String implementation() {
        return "opensearch";
    }

    @Override
    public Mono<List<DocumentSearchResult>> search(String workspaceId, String query, Pageable pageable) {
        return documentSearchRepository.findByWorkspaceIdAndTitleContainsOrContentContains(workspaceId, query, query, pageable)
                .map(documents -> documents
                        .stream()
                        .map(doc -> new DocumentSearchResult(
                                doc.getDocumentId(),
                                doc.getWorkspaceId(),
                                doc.getTitle(),
                                doc.getContent(),
                                doc.getStatus(),
                                doc.getCreatedByUserId(),
                                doc.getLastModifiedByUserId()
                        ))
                        .toList())
                .flatMap(results -> {
                    if (!results.isEmpty()) {
                        return Mono.just(results);
                    }
                    LOGGER.debug("OpenSearch returned no document hits, falling back to PostgreSQL ILIKE");
                    return fallbackSearch.search(workspaceId, query, pageable);
                })
                .onErrorResume(RuntimeException.class, exception -> {
                    LOGGER.warn("OpenSearch document search failed, falling back to PostgreSQL ILIKE: {}", exception.getMessage());
                    return fallbackSearch.search(workspaceId, query, pageable);
                });
    }
}
