package com.example.platform.documents.application.search;

import java.util.List;
import org.springframework.data.domain.Pageable;

public interface DocumentSearchUseCase {

    String version();

    String implementation();

    List<DocumentSearchResult> search(String workspaceId, String query, Pageable pageable);
}
