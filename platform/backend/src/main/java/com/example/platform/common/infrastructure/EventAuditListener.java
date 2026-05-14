package com.example.platform.common.infrastructure;

import com.example.platform.common.audit.AuditLogger;
import com.example.platform.documents.domain.DocumentCreatedEvent;
import com.example.platform.documents.domain.DocumentUpdatedEvent;
import com.example.platform.identityaccess.domain.WorkspaceMemberAddedEvent;
import com.example.platform.messaging.domain.MessagePostedEvent;
import com.example.platform.tasks.domain.TaskAssignedEvent;
import com.example.platform.tasks.domain.TaskCreatedEvent;
import com.example.platform.tasks.domain.TaskStatusChangedEvent;
import com.example.platform.tenantmanagement.domain.TenantCreatedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Listens to all domain events and logs them for audit compliance.
 * Phase 1b: In-process listener
 * Phase 2+: Replace with external audit service
 */
@Component
public class EventAuditListener {

    private final AuditLogger auditLogger;

    public EventAuditListener(AuditLogger auditLogger) {
        this.auditLogger = auditLogger;
    }

    @EventListener
    public void onTenantCreated(TenantCreatedEvent event) {
        auditLogger.logWrite(
                "events",
                "publish-tenant-created",
                "tenant",
                event.getAggregateId(),
                "SUCCESS"
        );
    }

    @EventListener
    public void onWorkspaceMemberAdded(WorkspaceMemberAddedEvent event) {
        auditLogger.logWrite(
                "events",
                "publish-member-added",
                "membership",
                event.getAggregateId(),
                "SUCCESS"
        );
    }

    @EventListener
    public void onMessagePosted(MessagePostedEvent event) {
        auditLogger.logWrite(
                "events",
                "publish-message-posted",
                "message",
                event.getAggregateId(),
                "SUCCESS"
        );
    }

    @EventListener
    public void onDocumentCreated(DocumentCreatedEvent event) {
        auditLogger.logWrite(
                "events",
                "publish-document-created",
                "document",
                event.getAggregateId(),
                "SUCCESS"
        );
    }

    @EventListener
    public void onDocumentUpdated(DocumentUpdatedEvent event) {
        auditLogger.logWrite(
                "events",
                "publish-document-updated",
                "document",
                event.getAggregateId(),
                "SUCCESS"
        );
    }

    @EventListener
    public void onTaskCreated(TaskCreatedEvent event) {
        auditLogger.logWrite(
                "events",
                "publish-task-created",
                "task",
                event.getAggregateId(),
                "SUCCESS"
        );
    }

    @EventListener
    public void onTaskAssigned(TaskAssignedEvent event) {
        auditLogger.logWrite(
                "events",
                "publish-task-assigned",
                "task",
                event.getAggregateId(),
                "SUCCESS"
        );
    }

    @EventListener
    public void onTaskStatusChanged(TaskStatusChangedEvent event) {
        auditLogger.logWrite(
                "events",
                "publish-task-status-changed",
                "task",
                event.getAggregateId(),
                "SUCCESS"
        );
    }
}


