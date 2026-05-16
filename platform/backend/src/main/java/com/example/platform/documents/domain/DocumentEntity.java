package com.example.platform.documents.domain;

import com.example.platform.common.domain.AbstractAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "documents", indexes = {
        @Index(name = "idx_documents_workspace", columnList = "workspace_id")
})
public class DocumentEntity extends AbstractAuditableEntity {

    @Id
    @Column(name = "document_id", nullable = false, length = 64)
    private String documentId;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Column(name = "workspace_id", nullable = false, length = 64)
    private String workspaceId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "content", nullable = false, length = 12000)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private DocumentStatus status;

    @Column(name = "created_by_user_id", nullable = false, length = 64)
    private String createdByUserId;

    @Column(name = "last_modified_by_user_id", nullable = false, length = 64)
    private String lastModifiedByUserId;

    protected DocumentEntity() {
    }

    public DocumentEntity(
            String documentId,
            String tenantId,
            String workspaceId,
            String title,
            String content,
            DocumentStatus status,
            String createdByUserId,
            String lastModifiedByUserId
    ) {
        this.documentId = documentId;
        this.tenantId = tenantId;
        this.workspaceId = workspaceId;
        this.title = title;
        this.content = content;
        this.status = status;
        this.createdByUserId = createdByUserId;
        this.lastModifiedByUserId = lastModifiedByUserId;
    }

    public String getDocumentId() {
        return documentId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getWorkspaceId() {
        return workspaceId;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public DocumentStatus getStatus() {
        return status;
    }

    public String getCreatedByUserId() {
        return createdByUserId;
    }

    public String getLastModifiedByUserId() {
        return lastModifiedByUserId;
    }

    public void update(String title, String content, DocumentStatus status, String modifiedByUserId) {
        this.title = title;
        this.content = content;
        this.status = status;
        this.lastModifiedByUserId = modifiedByUserId;
    }
}
