package com.example.platform.documents.infrastructure;

import com.example.platform.documents.domain.DocumentSearchDocument;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.opensearch.client.Request;
import org.opensearch.client.Response;
import org.opensearch.client.ResponseException;
import org.opensearch.client.RestClient;
import org.opensearch.client.RestClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

@Repository
public class DocumentSearchRepository {

    private static final Logger log = LoggerFactory.getLogger(DocumentSearchRepository.class);
    private static final String INDEX_NAME = "documents";

    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    private volatile boolean indexEnsured;

    public DocumentSearchRepository(
            ObjectMapper objectMapper,
            @Value("${spring.data.elasticsearch.uris:http://localhost:9200}") String uris,
            @Value("${platform.search.opensearch.connect-timeout:500ms}") Duration connectTimeout,
            @Value("${platform.search.opensearch.socket-timeout:1s}") Duration socketTimeout
    ) {
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder(org.apache.http.HttpHost.create(firstUri(uris)))
                .setRequestConfigCallback(requestConfigBuilder -> requestConfigBuilder
                        .setConnectTimeout(Math.toIntExact(connectTimeout.toMillis()))
                        .setSocketTimeout(Math.toIntExact(socketTimeout.toMillis())))
                .build();
    }

    public void save(DocumentSearchDocument document) {
        try {
            ensureIndex();

            Request request = new Request("PUT", "/" + INDEX_NAME + "/_doc/" + document.getDocumentId());
            request.addParameter("refresh", "wait_for");
            request.setJsonEntity(objectMapper.writeValueAsString(toSource(document)));
            restClient.performRequest(request);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to index document in OpenSearch", exception);
        }
    }

    public List<DocumentSearchDocument> findByWorkspaceIdAndTitleContainsOrContentContains(
            String workspaceId, String titleQuery, String contentQuery
    ) {
        String query = titleQuery == null || titleQuery.isBlank() ? contentQuery : titleQuery;
        Map<String, Object> payload = Map.of(
                "size", 50,
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

    public List<DocumentSearchDocument> findByWorkspaceId(String workspaceId) {
        return search(Map.of(
                "size", 100,
                "query", Map.of(
                        "term", Map.of("workspaceId", workspaceId)
                )
        ));
    }

    public List<DocumentSearchDocument> findByTenantId(String tenantId) {
        return search(Map.of(
                "size", 100,
                "query", Map.of(
                        "term", Map.of("tenantId", tenantId)
                )
        ));
    }

    @PreDestroy
    void close() throws IOException {
        restClient.close();
    }

    private List<DocumentSearchDocument> search(Map<String, Object> payload) {
        try {
            ensureIndex();

            Request request = new Request("POST", "/" + INDEX_NAME + "/_search");
            request.setJsonEntity(objectMapper.writeValueAsString(payload));
            Response response = restClient.performRequest(request);
            JsonNode root = objectMapper.readTree(response.getEntity().getContent());
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
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to search documents in OpenSearch", exception);
        }
    }

    private synchronized void ensureIndex() throws IOException {
        if (indexEnsured) {
            return;
        }

        try {
            restClient.performRequest(new Request("HEAD", "/" + INDEX_NAME));
            indexEnsured = true;
            return;
        } catch (ResponseException exception) {
            if (exception.getResponse().getStatusLine().getStatusCode() != 404) {
                throw exception;
            }
        }

        Request request = new Request("PUT", "/" + INDEX_NAME);
        request.setJsonEntity("""
                {
                  "settings": {
                    "number_of_shards": 1,
                    "number_of_replicas": 0
                  },
                  "mappings": {
                    "properties": {
                      "documentId": { "type": "keyword" },
                      "tenantId": { "type": "keyword" },
                      "workspaceId": { "type": "keyword" },
                      "title": { "type": "text" },
                      "content": { "type": "text" },
                      "status": { "type": "keyword" },
                      "createdByUserId": { "type": "keyword" },
                      "lastModifiedByUserId": { "type": "keyword" },
                      "createdAt": { "type": "date" },
                      "updatedAt": { "type": "date" }
                    }
                  }
                }
                """);
        restClient.performRequest(request);
        indexEnsured = true;
        log.info("Created OpenSearch index '{}'", INDEX_NAME);
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
