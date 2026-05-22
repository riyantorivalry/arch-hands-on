package com.example.platform.documents.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@ConditionalOnProperty(
        name = "platform.search.postgres.index-initializer.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class PostgresDocumentSearchIndexInitializer implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(PostgresDocumentSearchIndexInitializer.class);

    private final DatabaseClient databaseClient;

    public PostgresDocumentSearchIndexInitializer(DatabaseClient databaseClient) {
        this.databaseClient = databaseClient;
    }

    @Override
    public void run(ApplicationArguments args) {
        initialize()
                .subscribe(
                        ignored -> {
                        },
                        exception -> LOGGER.warn("Could not initialize PostgreSQL document search indexes: {}", exception.getMessage())
                );
    }

    Mono<Void> initialize() {
        return execute("create extension if not exists pg_trgm")
                .then(execute("""
                        create index if not exists idx_documents_search_tsv
                        on documents
                        using gin (to_tsvector('english', coalesce(title, '') || ' ' || coalesce(content, '')))
                        """))
                .then(execute("""
                        create index if not exists idx_documents_title_trgm
                        on documents
                        using gin (title gin_trgm_ops)
                        """))
                .then(execute("""
                        create index if not exists idx_documents_content_trgm
                        on documents
                        using gin (content gin_trgm_ops)
                        """))
                .then();
    }

    private Mono<Long> execute(String sql) {
        return databaseClient.sql(sql).fetch().rowsUpdated();
    }
}
