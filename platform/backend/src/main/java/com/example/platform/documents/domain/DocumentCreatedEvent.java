package com.example.platform.documents.domain;

import com.example.platform.common.domain.DomainEvent;

/**
 * Event published when a document is created.
 */
public class DocumentCreatedEvent extends DomainEvent {
    private final String workspaceId;
    private final String title;
    private final String createdByUserId;

    public DocumentCreatedEvent(String tenantId, String documentId, String workspaceId, String title, String createdByUserId) {
        super(tenantId, documentId);
        this.workspaceId = workspaceId;
        this.title = title;
        this.createdByUserId = createdByUserId;
    }

    @Override
    public String getEventType() {
        return "DocumentCreatedEvent";
    }

    public String getWorkspaceId() {
        return workspaceId;
    }

    public String getTitle() {
        return title;
    }

    public String getCreatedByUserId() {
        return createdByUserId;
    }
}
