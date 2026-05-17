package com.example.platform.documents.application.search;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PostgresFullTextDocumentSearch implements DocumentSearchUseCase {

    private static final Logger LOGGER = LoggerFactory.getLogger(PostgresFullTextDocumentSearch.class);

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;
    private final PostgresIlikeDocumentSearch fallbackSearch;

    public PostgresFullTextDocumentSearch(
            JdbcTemplate jdbcTemplate,
            DataSource dataSource,
            PostgresIlikeDocumentSearch fallbackSearch
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.dataSource = dataSource;
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
    @Transactional(readOnly = true)
    public List<DocumentSearchResult> search(String workspaceId, String query, Pageable pageable) {
        if (!isPostgres()) {
            return fallbackSearch.search(workspaceId, query, pageable);
        }

        try {
            return jdbcTemplate.query(
                    """
                            select document_id,
                                   workspace_id,
                                   title,
                                   content,
                                   status,
                                   created_by_user_id,
                                   last_modified_by_user_id
                            from documents
                            where workspace_id = ?
                              and (
                                to_tsvector('english', coalesce(title, '') || ' ' || coalesce(content, ''))
                                  @@ websearch_to_tsquery('english', ?)
                                or similarity(title, ?) > 0.20
                                or similarity(content, ?) > 0.20
                              )
                            order by
                              ts_rank(
                                to_tsvector('english', coalesce(title, '') || ' ' || coalesce(content, '')),
                                websearch_to_tsquery('english', ?)
                              ) desc,
                              greatest(similarity(title, ?), similarity(content, ?)) desc,
                              updated_at desc
                            limit ?
                            offset ?
                            """,
                    rowMapper(),
                    workspaceId,
                    query,
                    query,
                    query,
                    query,
                    query,
                    query,
                    pageable.getPageSize(),
                    pageable.getOffset()
            );
        } catch (RuntimeException exception) {
            LOGGER.warn("PostgreSQL full-text search failed, falling back to ILIKE: {}", exception.getMessage());
            return fallbackSearch.search(workspaceId, query, pageable);
        }
    }

    private boolean isPostgres() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.getMetaData().getDatabaseProductName().toLowerCase().contains("postgresql");
        } catch (SQLException exception) {
            LOGGER.warn("Could not detect database product, falling back to ILIKE search", exception);
            return false;
        }
    }

    private RowMapper<DocumentSearchResult> rowMapper() {
        return (ResultSet rs, int rowNum) -> new DocumentSearchResult(
                rs.getString("document_id"),
                rs.getString("workspace_id"),
                rs.getString("title"),
                rs.getString("content"),
                rs.getString("status"),
                rs.getString("created_by_user_id"),
                rs.getString("last_modified_by_user_id")
        );
    }
}
