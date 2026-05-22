package com.example.platform.documents.application.search;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class PostgresFullTextDocumentSearch implements DocumentSearchUseCase {

    private static final Logger LOGGER = LoggerFactory.getLogger(PostgresFullTextDocumentSearch.class);

    private final DatabaseClient databaseClient;
    private final PostgresIlikeDocumentSearch fallbackSearch;

    public PostgresFullTextDocumentSearch(
            DatabaseClient databaseClient,
            PostgresIlikeDocumentSearch fallbackSearch
    ) {
        this.databaseClient = databaseClient;
        this.fallbackSearch = fallbackSearch;
    }

    @Override
    public String version() {
        return "v2";
    }

    @Override
    public String implementation() {
        return "postgres-fts-pg-trgm";
    }

    @Override
    public Mono<List<DocumentSearchResult>> search(String workspaceId, String query, Pageable pageable) {
        return databaseClient.sql("""
                        select document_id,
                               workspace_id,
                               title,
                               content,
                               status,
                               created_by_user_id,
                               last_modified_by_user_id
                        from documents
                        where workspace_id = :workspaceId
                          and (
                            to_tsvector('english', coalesce(title, '') || ' ' || coalesce(content, ''))
                              @@ websearch_to_tsquery('english', :query)
                            or similarity(title, :query) > 0.20
                            or similarity(content, :query) > 0.20
                          )
                        order by
                          ts_rank(
                            to_tsvector('english', coalesce(title, '') || ' ' || coalesce(content, '')),
                            websearch_to_tsquery('english', :query)
                          ) desc,
                          greatest(similarity(title, :query), similarity(content, :query)) desc,
                          updated_at desc
                        limit :limit
                        offset :offset
                        """)
                .bind("workspaceId", workspaceId)
                .bind("query", query)
                .bind("limit", pageable.getPageSize())
                .bind("offset", pageable.getOffset())
                .map((row, metadata) -> new DocumentSearchResult(
                        row.get("document_id", String.class),
                        row.get("workspace_id", String.class),
                        row.get("title", String.class),
                        row.get("content", String.class),
                        row.get("status", String.class),
                        row.get("created_by_user_id", String.class),
                        row.get("last_modified_by_user_id", String.class)
                ))
                .all()
                .collectList()
                .onErrorResume(RuntimeException.class, exception -> {
                    LOGGER.warn("PostgreSQL full-text search failed, falling back to ILIKE: {}", exception.getMessage());
                    return fallbackSearch.search(workspaceId, query, pageable);
                });
    }
}
