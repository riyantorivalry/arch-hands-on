package com.example.platform.documents.application.search;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class DocumentSearchComparisonService {

    private final Map<String, DocumentSearchUseCase> searchesByVersion;

    public DocumentSearchComparisonService(List<DocumentSearchUseCase> searchUseCases) {
        this.searchesByVersion = searchUseCases.stream()
                .collect(Collectors.toUnmodifiableMap(DocumentSearchUseCase::version, Function.identity()));
    }

    public Mono<List<DocumentSearchResult>> search(String version, String workspaceId, String query, Pageable pageable) {
        DocumentSearchUseCase search = searchesByVersion.get(version);
        if (search == null) {
            return Mono.error(new IllegalArgumentException("Unsupported document search API version: " + version));
        }
        return search.search(workspaceId, query, pageable);
    }
}
