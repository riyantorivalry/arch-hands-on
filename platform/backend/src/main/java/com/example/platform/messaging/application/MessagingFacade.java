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
import reactor.core.publisher.Mono;

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

    @Transactional(transactionManager = "connectionFactoryTransactionManager")
    public Mono<ChannelView> createChannel(String workspaceId, String actorUserId, String channelName) {
        return requireActiveMembership(workspaceId, actorUserId)
                .flatMap(membership -> {
                    return authorizationService.requireWorkspaceManager(membership)
                            .then(workspaceRepository.findById(workspaceId))
                            .switchIfEmpty(Mono.error(new IllegalArgumentException("Workspace not found: " + workspaceId)))
                            .flatMap(workspace -> channelRepository.save(new ChannelEntity(
                                    "channel-" + slugify(channelName),
                                    membership.getTenantId(),
                                    workspace.getWorkspaceId(),
                                    channelName
                            )));
                })
                .doOnNext(channel -> auditLogger.logWrite("messaging", "create", "channel", channel.getChannelId(), "SUCCESS"))
                .map(this::toChannelView);
    }

    @Transactional(transactionManager = "connectionFactoryTransactionManager", readOnly = true)
    public Mono<List<ChannelView>> listChannels(String workspaceId) {
        return listChannels(workspaceId, null, 0, DEFAULT_PAGE_SIZE);
    }

    @Transactional(transactionManager = "connectionFactoryTransactionManager", readOnly = true)
    public Mono<List<ChannelView>> listChannels(String workspaceId, String userId, int page, int size) {
        Mono<Void> authorized = userId == null ? Mono.empty() : requireActiveMembership(workspaceId, userId).then();
        Pageable pageable = pageRequest(page, size);
        return authorized.thenMany(channelRepository.findByWorkspaceIdOrderByNameAsc(
                        workspaceId,
                        pageable.getPageSize(),
                        pageable.getOffset()
                ))
                .map(this::toChannelView)
                .collectList();
    }

    @Transactional(transactionManager = "connectionFactoryTransactionManager")
    public Mono<MessageView> postMessage(String workspaceId, String channelId, String authorUserId, String body) {
        return requireActiveMembership(workspaceId, authorUserId)
                .flatMap(membership -> channelRepository.findByChannelIdAndWorkspaceId(channelId, workspaceId)
                        .switchIfEmpty(Mono.error(new IllegalArgumentException("Channel not found in workspace " + workspaceId)))
                        .flatMap(channel -> messageRepository.save(new MessageEntity(
                                        "message-" + UUID.randomUUID(),
                                        membership.getTenantId(),
                                        workspaceId,
                                        channel.getChannelId(),
                                        authorUserId,
                                        body,
                                        null
                                ))
                                .flatMap(saved -> {
                                    auditLogger.logWrite("messaging", "create", "message", saved.getMessageId(), "SUCCESS");
                                    return domainEventPublisher.publish(new MessagePostedEvent(
                                            membership.getTenantId(),
                                            saved.getMessageId(),
                                            channel.getChannelId(),
                                            authorUserId,
                                            body
                                    )).thenReturn(saved);
                                })))
                .map(this::toMessageView);
    }

    @Transactional(transactionManager = "connectionFactoryTransactionManager", readOnly = true)
    public Mono<List<MessageView>> listMessages(String channelId) {
        return listMessages(channelId, null, 0, DEFAULT_PAGE_SIZE);
    }

    @Transactional(transactionManager = "connectionFactoryTransactionManager", readOnly = true)
    public Mono<List<MessageView>> listMessages(String channelId, String userId, int page, int size) {
        Pageable pageable = pageRequest(page, size);
        return channelRepository.findById(channelId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Channel not found: " + channelId)))
                .flatMapMany(channel -> {
                    Mono<Void> authorized = userId == null ? Mono.empty() : requireActiveMembership(channel.getWorkspaceId(), userId).then();
                    return authorized.thenMany(messageRepository.findByChannelIdOrderByCreatedAtAsc(
                            channelId,
                            pageable.getPageSize(),
                            pageable.getOffset()
                    ));
                })
                .map(this::toMessageView)
                .collectList();
    }

    @Transactional(transactionManager = "connectionFactoryTransactionManager")
    public Mono<MessageView> replyToMessage(String workspaceId, String messageId, String authorUserId, String body) {
        return requireActiveMembership(workspaceId, authorUserId)
                .flatMap(membership -> messageRepository.findById(messageId)
                        .switchIfEmpty(Mono.error(new IllegalArgumentException("Parent message not found: " + messageId)))
                        .flatMap(parent -> {
                            if (!workspaceId.equals(parent.getWorkspaceId())) {
                                return Mono.error(new IllegalArgumentException("Parent message not found in workspace " + workspaceId));
                            }
                            return messageRepository.save(new MessageEntity(
                                            "message-" + UUID.randomUUID(),
                                            membership.getTenantId(),
                                            workspaceId,
                                            parent.getChannelId(),
                                            authorUserId,
                                            body,
                                            parent.getMessageId()
                                    ))
                                    .flatMap(saved -> {
                                        auditLogger.logWrite("messaging", "reply", "message", saved.getMessageId(), "SUCCESS");
                                        return domainEventPublisher.publish(new MessagePostedEvent(
                                                membership.getTenantId(),
                                                saved.getMessageId(),
                                                parent.getChannelId(),
                                                authorUserId,
                                                body
                                        )).thenReturn(saved);
                                    });
                        }))
                .map(this::toMessageView);
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

    private Mono<com.example.platform.identityaccess.domain.MembershipEntity> requireActiveMembership(String workspaceId, String userId) {
        return membershipRepository.findByWorkspaceIdAndUserId(workspaceId, userId)
                .switchIfEmpty(Mono.error(new AuthorizationDeniedException("User is not a member of workspace " + workspaceId)))
                .flatMap(membership -> {
                    if (membership.getStatus() != MembershipStatus.ACTIVE) {
                        return Mono.error(new IllegalStateException("Membership is not active for user " + userId));
                    }
                    return Mono.just(membership);
                });
    }

    private Pageable pageRequest(int page, int size) {
        int normalizedPage = Math.max(page, 0);
        int normalizedSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        return PageRequest.of(normalizedPage, normalizedSize);
    }
}
