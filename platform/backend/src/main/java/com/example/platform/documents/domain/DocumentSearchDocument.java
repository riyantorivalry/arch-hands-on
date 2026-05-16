package com.example.platform.documents.domain;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

/**
 * OpenSearch document representation for full-text search over documents.
 * Indexes document content and metadata for efficient searching.
 */
@Document(indexName = "documents")
public class DocumentSearchDocument {

    @Id
    private String documentId;

    @Field(type = FieldType.Keyword)
    private String tenantId;

    @Field(type = FieldType.Keyword)
    private String workspaceId;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String title;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String content;

    @Field(type = FieldType.Keyword)
    private String status;

    @Field(type = FieldType.Keyword)
    private String createdByUserId;

    @Field(type = FieldType.Keyword)
    private String lastModifiedByUserId;

    @Field(type = FieldType.Date)
    private Instant createdAt;

    @Field(type = FieldType.Date)
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

