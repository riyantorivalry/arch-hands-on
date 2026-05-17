package com.example.platform.messaging.application;

import com.example.platform.common.audit.AuditLogger;
import com.example.platform.common.domain.DomainEventPublisher;
import com.example.platform.common.web.AuthorizationDeniedException;
import com.example.platform.identityaccess.application.AuthorizationService;
import com.example.platform.identityaccess.domain.MembershipStatus;
import com.example.platform.identityaccess.infrastructure.MembershipRepository;
import com.example.platform.messaging.domain.ChannelEntity;
import com.example.platform.messaging.domain.MessageEntity;
import com.example.platform.messaging.domain.MessagePostedEvent;
import com.example.platform.tenantmanagement.infrastructure.WorkspaceRepository;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MessagingFacade {

    private static final int DEFAULT_PAGE_SIZE = 50;
    private static final int MAX_PAGE_SIZE = 100;

    private final com.example.platform.messaging.infrastructure.ChannelRepository channelRepository;
    private final com.example.platform.messaging.infrastructure.MessageRepository messageRepository;
    private final MembershipRepository membershipRepository;
    private final WorkspaceRepository workspaceRepository;
    private final AuditLogger auditLogger;
    private final AuthorizationService authorizationService;
    private final DomainEventPublisher domainEventPublisher;

    public MessagingFacade(
            com.example.platform.messaging.infrastructure.ChannelRepository channelRepository,
            com.example.platform.messaging.infrastructure.MessageRepository messageRepository,
            MembershipRepository membershipRepository,
            WorkspaceRepository workspaceRepository,
            AuditLogger auditLogger,
            AuthorizationService authorizationService,
            DomainEventPublisher domainEventPublisher
    ) {
        this.channelRepository = channelRepository;
        this.messageRepository = messageRepository;
        this.membershipRepository = membershipRepository;
        this.workspaceRepository = workspaceRepository;
        this.auditLogger = auditLogger;
        this.authorizationService = authorizationService;
        this.domainEventPublisher = domainEventPublisher;
    }

    @Transactional
    public ChannelView createChannel(String workspaceId, String actorUserId, String channelName) {
        var membership = requireActiveMembership(workspaceId, actorUserId);
        authorizationService.requireWorkspaceManager(membership);
        var workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found: " + workspaceId));
        ChannelEntity channel = channelRepository.save(new ChannelEntity(
                "channel-" + slugify(channelName),
                membership.getTenantId(),
                workspace.getWorkspaceId(),
                channelName
        ));
        auditLogger.logWrite("messaging", "create", "channel", channel.getChannelId(), "SUCCESS");
        return toChannelView(channel);
    }

    @Transactional(readOnly = true)
    public List<ChannelView> listChannels(String workspaceId) {
        return listChannels(workspaceId, null, 0, DEFAULT_PAGE_SIZE);
    }

    @Transactional(readOnly = true)
    public List<ChannelView> listChannels(String workspaceId, String userId, int page, int size) {
        if (userId != null) {
            requireActiveMembership(workspaceId, userId);
        }
        return channelRepository.findByWorkspaceIdOrderByNameAsc(workspaceId, pageRequest(page, size)).stream()
                .map(this::toChannelView)
                .toList();
    }

    @Transactional
    public MessageView postMessage(String workspaceId, String channelId, String authorUserId, String body) {
        var membership = requireActiveMembership(workspaceId, authorUserId);
        var channel = channelRepository.findByChannelIdAndWorkspaceId(channelId, workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Channel not found in workspace " + workspaceId));
        MessageEntity saved = messageRepository.save(new MessageEntity(
                "message-" + UUID.randomUUID(),
                membership.getTenantId(),
                workspaceId,
                channel.getChannelId(),
                authorUserId,
                body,
                null
        ));
        auditLogger.logWrite("messaging", "create", "message", saved.getMessageId(), "SUCCESS");
        
        // Publish domain event
        domainEventPublisher.publish(new MessagePostedEvent(
                membership.getTenantId(),
                channelId,
                saved.getMessageId(),
                authorUserId,
                body
        ));
        
        return toMessageView(saved);
    }

    @Transactional(readOnly = true)
    public List<MessageView> listMessages(String channelId) {
        return listMessages(channelId, null, 0, DEFAULT_PAGE_SIZE);
    }

    @Transactional(readOnly = true)
    public List<MessageView> listMessages(String channelId, String userId, int page, int size) {
        var channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new IllegalArgumentException("Channel not found: " + channelId));
        if (userId != null) {
            requireActiveMembership(channel.getWorkspaceId(), userId);
        }
        return messageRepository.findByChannelIdOrderByCreatedAtAsc(channelId, pageRequest(page, size)).stream()
                .map(this::toMessageView)
                .toList();
    }

    @Transactional
    public MessageView replyToMessage(String workspaceId, String messageId, String authorUserId, String body) {
        var membership = requireActiveMembership(workspaceId, authorUserId);
        var parent = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Parent message not found: " + messageId));
        if (!workspaceId.equals(parent.getWorkspaceId())) {
            throw new IllegalArgumentException("Parent message not found in workspace " + workspaceId);
        }
        MessageEntity saved = messageRepository.save(new MessageEntity(
                "message-" + UUID.randomUUID(),
                membership.getTenantId(),
                workspaceId,
                parent.getChannelId(),
                authorUserId,
                body,
                parent.getMessageId()
        ));
        auditLogger.logWrite("messaging", "reply", "message", saved.getMessageId(), "SUCCESS");
        
        // Publish domain event
        domainEventPublisher.publish(new MessagePostedEvent(
                membership.getTenantId(),
                parent.getChannelId(),
                saved.getMessageId(),
                authorUserId,
                body
        ));
        
        return toMessageView(saved);
    }

    public record ChannelView(String channelId, String workspaceId, String name) {
    }

    public record MessageView(String messageId, String channelId, String authorUserId, String body, String parentMessageId) {
    }

    private ChannelView toChannelView(ChannelEntity channel) {
        return new ChannelView(channel.getChannelId(), channel.getWorkspaceId(), channel.getName());
    }

    private MessageView toMessageView(MessageEntity message) {
        return new MessageView(
                message.getMessageId(),
                message.getChannelId(),
                message.getAuthorUserId(),
                message.getBody(),
                message.getParentMessageId()
        );
    }

    private String slugify(String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return normalized.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }

    private com.example.platform.identityaccess.domain.MembershipEntity requireActiveMembership(String workspaceId, String userId) {
        var membership = membershipRepository.findByWorkspaceIdAndUserId(workspaceId, userId)
                .orElseThrow(() -> new AuthorizationDeniedException("User is not a member of workspace " + workspaceId));
        if (membership.getStatus() != MembershipStatus.ACTIVE) {
            throw new IllegalStateException("Membership is not active for user " + userId);
        }
        return membership;
    }

    private Pageable pageRequest(int page, int size) {
        int normalizedPage = Math.max(page, 0);
        int normalizedSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        return PageRequest.of(normalizedPage, normalizedSize);
    }
}
