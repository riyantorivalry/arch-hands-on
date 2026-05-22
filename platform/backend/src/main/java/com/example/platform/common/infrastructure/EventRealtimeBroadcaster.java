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
import com.example.platform.messaging.infrastructure.ChannelRepository;
import com.example.platform.realtime.application.RealtimeEventService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Records domain events and broadcasts them to realtime clients.
 */
@Component
@ConditionalOnBean(RealtimeEventHandler.class)
public class EventRealtimeBroadcaster {

    private final RealtimeEventHandler realtimeEventHandler;
    private final RealtimeEventService realtimeEventService;
    private final ChannelRepository channelRepository;

    public EventRealtimeBroadcaster(
            RealtimeEventHandler realtimeEventHandler,
            RealtimeEventService realtimeEventService,
            ChannelRepository channelRepository
    ) {
        this.realtimeEventHandler = realtimeEventHandler;
        this.realtimeEventService = realtimeEventService;
        this.channelRepository = channelRepository;
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
        channelRepository.findById(event.getChannelId())
                .map(channel -> channel.getWorkspaceId())
                .defaultIfEmpty("")
                .subscribe(workspaceId -> broadcastEventToWorkspace(workspaceId.isBlank() ? null : workspaceId, event.getTenantId(), event));
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
        var realtimeEvent = realtimeEventService.append(tenantId, workspaceId, event);
        realtimeEventHandler.broadcastEvent(realtimeEvent);
    }

    private void broadcastEventToTenant(String tenantId, DomainEvent event) {
        broadcastEventToWorkspace(null, tenantId, event);
    }
}
