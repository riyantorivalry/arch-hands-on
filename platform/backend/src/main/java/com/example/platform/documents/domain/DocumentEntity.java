package com.example.platform.documents.domain;

import com.example.platform.common.domain.AbstractAuditableEntity;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("documents")
public class DocumentEntity extends AbstractAuditableEntity {

    @Id
    @Column("document_id")
    private String documentId;

    @Column("tenant_id")
    private String tenantId;

    @Column("workspace_id")
    private String workspaceId;

    @Column("title")
    private String title;

    @Column("content")
    private String content;

    @Column("status")
    private DocumentStatus status;

    @Column("created_by_user_id")
    private String createdByUserId;

    @Column("last_modified_by_user_id")
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

    @Override
    public Object getId() {
        return documentId;
    }
}
