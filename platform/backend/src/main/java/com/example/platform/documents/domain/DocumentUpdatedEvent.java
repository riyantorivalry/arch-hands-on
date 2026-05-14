package com.example.platform.documents.domain;

import com.example.platform.common.domain.DomainEvent;

/**
 * Event published when a document is updated.
 */
public class DocumentUpdatedEvent extends DomainEvent {
    private final String workspaceId;
    private final String title;
    private final String lastModifiedByUserId;

    public DocumentUpdatedEvent(String tenantId, String documentId, String workspaceId, String title, String lastModifiedByUserId) {
        super(tenantId, documentId);
        this.workspaceId = workspaceId;
        this.title = title;
        this.lastModifiedByUserId = lastModifiedByUserId;
    }

    @Override
    public String getEventType() {
        return "DocumentUpdatedEvent";
    }

    public String getWorkspaceId() {
        return workspaceId;
    }

    public String getTitle() {
        return title;
    }

    public String getLastModifiedByUserId() {
        return lastModifiedByUserId;
    }
}
