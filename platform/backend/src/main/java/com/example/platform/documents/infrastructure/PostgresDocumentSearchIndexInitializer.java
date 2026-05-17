package com.example.platform.documents.infrastructure;

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = "platform.search.postgres.index-initializer.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class PostgresDocumentSearchIndexInitializer implements InitializingBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(PostgresDocumentSearchIndexInitializer.class);

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    public PostgresDocumentSearchIndexInitializer(DataSource dataSource, JdbcTemplate jdbcTemplate) {
        this.dataSource = dataSource;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void afterPropertiesSet() {
        if (!isPostgres()) {
            return;
        }

        try {
            jdbcTemplate.execute("create extension if not exists pg_trgm");
            jdbcTemplate.execute("""
                    create index if not exists idx_documents_search_tsv
                    on documents
                    using gin (to_tsvector('english', coalesce(title, '') || ' ' || coalesce(content, '')))
                    """);
            jdbcTemplate.execute("""
                    create index if not exists idx_documents_title_trgm
                    on documents
                    using gin (title gin_trgm_ops)
                    """);
            jdbcTemplate.execute("""
                    create index if not exists idx_documents_content_trgm
                    on documents
                    using gin (content gin_trgm_ops)
                    """);
        } catch (RuntimeException exception) {
            LOGGER.warn("Could not initialize PostgreSQL document search indexes: {}", exception.getMessage());
        }
    }

    private boolean isPostgres() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.getMetaData().getDatabaseProductName().toLowerCase().contains("postgresql");
        } catch (SQLException exception) {
            LOGGER.warn("Could not detect database product for document search index initialization", exception);
            return false;
        }
    }
}
