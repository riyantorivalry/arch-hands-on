package com.example.platform.documents.application.search;

public record DocumentSearchResult(
        String documentId,
        String workspaceId,
        String title,
        String content,
        String status,
        String createdByUserId,
        String lastModifiedByUserId
) {
}
