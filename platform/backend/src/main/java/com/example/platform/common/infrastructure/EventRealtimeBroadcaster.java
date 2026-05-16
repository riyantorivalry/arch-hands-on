package com.example.platform.common.infrastructure;

import com.example.platform.common.domain.DomainEvent;
import com.example.platform.common.web.RealtimeEventHandler;
import com.example.platform.documents.domain.DocumentCreatedEvent;
import com.example.platform.documents.domain.DocumentUpdatedEvent;
import com.example.platform.identityaccess.domain.WorkspaceMemberAddedEvent;
import com.example.platform.messaging.domain.MessagePostedEvent;
import com.example.platform.tasks.domain.TaskAssignedEvent;
import com.example.platform.tasks.domain.TaskCreatedEvent;
import com.example.platform.tasks.domain.TaskStatusChangedEvent;
import com.example.platform.tenantmanagement.domain.TenantCreatedEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Broadcasts domain events to WebSocket clients for realtime updates.
 * Phase 2: Sends events to subscribed clients as they happen.
 */
@Component
@ConditionalOnBean(RealtimeEventHandler.class)
public class EventRealtimeBroadcaster {

    private final RealtimeEventHandler realtimeEventHandler;

    public EventRealtimeBroadcaster(RealtimeEventHandler realtimeEventHandler) {
        this.realtimeEventHandler = realtimeEventHandler;
    }

    @EventListener
    public void onTenantCreated(TenantCreatedEvent event) {
        broadcastEventToWorkspace(event.getWorkspaceId(), event.getTenantId(), event);
    }

    @EventListener
    public void onWorkspaceMemberAdded(WorkspaceMemberAddedEvent event) {
        broadcastEventToWorkspace(event.getWorkspaceId(), event.getTenantId(), event);
    }

    @EventListener
    public void onMessagePosted(MessagePostedEvent event) {
        // For messages, we need to determine workspace from channel
        // For Phase 2, we'll broadcast to tenant scope for now
        // TODO: Add workspaceId to MessagePostedEvent or look up from channel
        broadcastEventToTenant(event.getTenantId(), event);
    }

    @EventListener
    public void onDocumentCreated(DocumentCreatedEvent event) {
        broadcastEventToWorkspace(event.getWorkspaceId(), event.getTenantId(), event);
    }

    @EventListener
    public void onDocumentUpdated(DocumentUpdatedEvent event) {
        broadcastEventToWorkspace(event.getWorkspaceId(), event.getTenantId(), event);
    }

    @EventListener
    public void onTaskCreated(TaskCreatedEvent event) {
        broadcastEventToWorkspace(event.getWorkspaceId(), event.getTenantId(), event);
    }

    @EventListener
    public void onTaskAssigned(TaskAssignedEvent event) {
        broadcastEventToWorkspace(event.getWorkspaceId(), event.getTenantId(), event);
    }

    @EventListener
    public void onTaskStatusChanged(TaskStatusChangedEvent event) {
        broadcastEventToWorkspace(event.getWorkspaceId(), event.getTenantId(), event);
    }

    private void broadcastEventToWorkspace(String workspaceId, String tenantId, DomainEvent event) {
        realtimeEventHandler.broadcastEvent(workspaceId, tenantId, event);
    }

    private void broadcastEventToTenant(String tenantId, DomainEvent event) {
        realtimeEventHandler.broadcastEvent(null, tenantId, event);
    }
}
