package com.example.platform.documents.domain;

import java.time.Instant;

/**
 * OpenSearch document representation for full-text search over documents.
 * Indexes document content and metadata for efficient searching.
 */
public class DocumentSearchDocument {

    private String documentId;
    private String tenantId;
    private String workspaceId;
    private String title;
    private String content;
    private String status;
    private String createdByUserId;
    private String lastModifiedByUserId;
    private Instant createdAt;
    private Instant updatedAt;

    protected DocumentSearchDocument() {
    }

    public DocumentSearchDocument(String documentId, String tenantId, String workspaceId, String title,
                                   String content, String status, String createdByUserId,
                                   String lastModifiedByUserId, Instant createdAt, Instant updatedAt) {
        this.documentId = documentId;
        this.tenantId = tenantId;
        this.workspaceId = workspaceId;
        this.title = title;
        this.content = content;
        this.status = status;
        this.createdByUserId = createdByUserId;
        this.lastModifiedByUserId = lastModifiedByUserId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters
    public String getDocumentId() { return documentId; }
    public String getTenantId() { return tenantId; }
    public String getWorkspaceId() { return workspaceId; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getStatus() { return status; }
    public String getCreatedByUserId() { return createdByUserId; }
    public String getLastModifiedByUserId() { return lastModifiedByUserId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}

