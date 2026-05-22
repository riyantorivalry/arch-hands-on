package com.example.platform.documents.infrastructure;

import com.example.platform.documents.domain.DocumentSearchDocument;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Repository
public class DocumentSearchRepository {

    private static final Logger log = LoggerFactory.getLogger(DocumentSearchRepository.class);
    private static final String INDEX_NAME = "documents";

    private final WebClient webClient;

    private volatile boolean indexEnsured;

    public DocumentSearchRepository(
            WebClient.Builder webClientBuilder,
            @Value("${spring.data.elasticsearch.uris:http://localhost:9200}") String uris,
            @Value("${platform.search.opensearch.connect-timeout:500ms}") Duration connectTimeout,
            @Value("${platform.search.opensearch.socket-timeout:1s}") Duration socketTimeout
    ) {
        this.webClient = webClientBuilder
                .baseUrl(firstUri(uris))
                .build();
    }

    public Mono<Void> save(DocumentSearchDocument document) {
        return ensureIndex()
                .then(webClient.put()
                        .uri("/" + INDEX_NAME + "/_doc/{id}?refresh=wait_for", document.getDocumentId())
                        .bodyValue(toSource(document))
                        .retrieve()
                        .bodyToMono(JsonNode.class))
                .then()
                .onErrorMap(exception -> new IllegalStateException("Failed to index document in OpenSearch", exception));
    }

    public Mono<List<DocumentSearchDocument>> findByWorkspaceIdAndTitleContainsOrContentContains(
            String workspaceId, String titleQuery, String contentQuery
    ) {
        return findByWorkspaceIdAndTitleContainsOrContentContains(
                workspaceId,
                titleQuery,
                contentQuery,
                Pageable.ofSize(50)
        );
    }

    public Mono<List<DocumentSearchDocument>> findByWorkspaceIdAndTitleContainsOrContentContains(
            String workspaceId, String titleQuery, String contentQuery, Pageable pageable
    ) {
        String query = titleQuery == null || titleQuery.isBlank() ? contentQuery : titleQuery;
        Map<String, Object> payload = Map.of(
                "from", Math.toIntExact(pageable.getOffset()),
                "size", pageable.getPageSize(),
                "query", Map.of(
                        "bool", Map.of(
                                "filter", List.of(
                                        Map.of("term", Map.of("workspaceId", workspaceId))
                                ),
                                "must", List.of(
                                        Map.of(
                                                "bool", Map.of(
                                                        "should", List.of(
                                                                Map.of("match", Map.of("title", Map.of("query", query))),
                                                                Map.of("match", Map.of("content", Map.of("query", query)))
                                                        ),
                                                        "minimum_should_match", 1
                                                )
                                        )
                                )
                        )
                )
        );
        return search(payload);
    }

    public Mono<List<DocumentSearchDocument>> findByWorkspaceId(String workspaceId) {
        return search(Map.of(
                "size", 100,
                "query", Map.of(
                        "term", Map.of("workspaceId", workspaceId)
                )
        ));
    }

    public Mono<List<DocumentSearchDocument>> findByTenantId(String tenantId) {
        return search(Map.of(
                "size", 100,
                "query", Map.of(
                        "term", Map.of("tenantId", tenantId)
                )
        ));
    }

    private Mono<List<DocumentSearchDocument>> search(Map<String, Object> payload) {
        return ensureIndex()
                .then(webClient.post()
                        .uri("/" + INDEX_NAME + "/_search")
                        .bodyValue(payload)
                        .retrieve()
                        .bodyToMono(JsonNode.class))
                .map(this::toDocuments)
                .onErrorMap(exception -> new IllegalStateException("Failed to search documents in OpenSearch", exception));
    }

    private Mono<Void> ensureIndex() {
        if (indexEnsured) {
            return Mono.empty();
        }

        return webClient.head()
                .uri("/" + INDEX_NAME)
                .exchangeToMono(response -> {
                    if (response.statusCode().is2xxSuccessful()) {
                        indexEnsured = true;
                        return Mono.empty();
                    }
                    if (response.statusCode() == HttpStatus.NOT_FOUND) {
                        return createIndex();
                    }
                    return error(response, "Failed to inspect OpenSearch index");
                });
    }

    private Mono<Void> createIndex() {
        return webClient.put()
                .uri("/" + INDEX_NAME)
                .bodyValue(Map.of(
                        "settings", Map.of(
                                "number_of_shards", 1,
                                "number_of_replicas", 0
                        ),
                        "mappings", Map.of(
                                "properties", Map.ofEntries(
                                        Map.entry("documentId", Map.of("type", "keyword")),
                                        Map.entry("tenantId", Map.of("type", "keyword")),
                                        Map.entry("workspaceId", Map.of("type", "keyword")),
                                        Map.entry("title", Map.of("type", "text")),
                                        Map.entry("content", Map.of("type", "text")),
                                        Map.entry("status", Map.of("type", "keyword")),
                                        Map.entry("createdByUserId", Map.of("type", "keyword")),
                                        Map.entry("lastModifiedByUserId", Map.of("type", "keyword")),
                                        Map.entry("createdAt", Map.of("type", "date")),
                                        Map.entry("updatedAt", Map.of("type", "date"))
                                )
                        )
                ))
                .exchangeToMono(response -> {
                    if (response.statusCode().is2xxSuccessful()) {
                        indexEnsured = true;
                        log.info("Created OpenSearch index '{}'", INDEX_NAME);
                        return Mono.empty();
                    }
                    return error(response, "Failed to create OpenSearch index");
                });
    }

    private List<DocumentSearchDocument> toDocuments(JsonNode root) {
        JsonNode hits = root.path("hits").path("hits");
        List<DocumentSearchDocument> results = new ArrayList<>();
        for (JsonNode hit : hits) {
            JsonNode source = hit.path("_source");
            results.add(new DocumentSearchDocument(
                    source.path("documentId").asText(),
                    source.path("tenantId").asText(),
                    source.path("workspaceId").asText(),
                    source.path("title").asText(),
                    source.path("content").asText(),
                    source.path("status").asText(),
                    source.path("createdByUserId").asText(),
                    source.path("lastModifiedByUserId").asText(),
                    readInstant(source, "createdAt"),
                    readInstant(source, "updatedAt")
            ));
        }
        return results;
    }

    private Map<String, Object> toSource(DocumentSearchDocument document) {
        Map<String, Object> source = new LinkedHashMap<>();
        source.put("documentId", document.getDocumentId());
        source.put("tenantId", document.getTenantId());
        source.put("workspaceId", document.getWorkspaceId());
        source.put("title", document.getTitle());
        source.put("content", document.getContent());
        source.put("status", document.getStatus());
        source.put("createdByUserId", document.getCreatedByUserId());
        source.put("lastModifiedByUserId", document.getLastModifiedByUserId());
        source.put("createdAt", writeInstant(document.getCreatedAt()));
        source.put("updatedAt", writeInstant(document.getUpdatedAt()));
        return source;
    }

    private Mono<Void> error(ClientResponse response, String message) {
        return response.bodyToMono(String.class)
                .defaultIfEmpty("")
                .flatMap(body -> Mono.error(new IllegalStateException(message + ": " + response.statusCode() + " " + body)));
    }

    private static String firstUri(String uris) {
        return List.of(uris.split(",")).stream()
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .findFirst()
                .orElse("http://localhost:9200");
    }

    private static Instant readInstant(JsonNode source, String fieldName) {
        JsonNode value = source.path(fieldName);
        return value.isMissingNode() || value.isNull() || value.asText().isBlank()
                ? null
                : Instant.parse(value.asText());
    }

    private static String writeInstant(Instant value) {
        return value == null ? null : value.toString();
    }
}
