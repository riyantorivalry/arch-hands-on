package com.example.platform.documents.application.search;

import java.util.List;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Mono;

public interface DocumentSearchUseCase {

    String version();

    String implementation();

    Mono<List<DocumentSearchResult>> search(String workspaceId, String query, Pageable pageable);
}
