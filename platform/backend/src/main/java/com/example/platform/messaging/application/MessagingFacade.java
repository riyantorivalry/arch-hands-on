package com.example.platform.messaging.application;

import com.example.platform.identityaccess.infrastructure.MembershipRepository;
import com.example.platform.messaging.domain.ChannelEntity;
import com.example.platform.messaging.domain.MessageEntity;
import com.example.platform.messaging.infrastructure.ChannelRepository;
import com.example.platform.messaging.infrastructure.MessageRepository;
import com.example.platform.tenantmanagement.infrastructure.WorkspaceRepository;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MessagingFacade {

    private final ChannelRepository channelRepository;
    private final MessageRepository messageRepository;
    private final MembershipRepository membershipRepository;
    private final WorkspaceRepository workspaceRepository;

    public MessagingFacade(
            ChannelRepository channelRepository,
            MessageRepository messageRepository,
            MembershipRepository membershipRepository,
            WorkspaceRepository workspaceRepository
    ) {
        this.channelRepository = channelRepository;
        this.messageRepository = messageRepository;
        this.membershipRepository = membershipRepository;
        this.workspaceRepository = workspaceRepository;
    }

    @Transactional
    public ChannelView createChannel(String workspaceId, String actorUserId, String channelName) {
        var membership = membershipRepository.findByWorkspaceIdAndUserId(workspaceId, actorUserId)
                .orElseThrow(() -> new IllegalArgumentException("User is not a member of workspace " + workspaceId));
        var workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found: " + workspaceId));
        ChannelEntity channel = channelRepository.save(new ChannelEntity(
                "channel-" + slugify(channelName),
                membership.getTenantId(),
                workspace.getWorkspaceId(),
                channelName
        ));
        return toChannelView(channel);
    }

    public List<ChannelView> listChannels(String workspaceId) {
        return channelRepository.findByWorkspaceIdOrderByNameAsc(workspaceId).stream()
                .map(this::toChannelView)
                .toList();
    }

    @Transactional
    public MessageView postMessage(String workspaceId, String channelId, String authorUserId, String body) {
        var membership = membershipRepository.findByWorkspaceIdAndUserId(workspaceId, authorUserId)
                .orElseThrow(() -> new IllegalArgumentException("User is not a member of workspace " + workspaceId));
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
        return toMessageView(saved);
    }

    public List<MessageView> listMessages(String channelId) {
        return messageRepository.findByChannelIdOrderByCreatedAtAsc(channelId).stream()
                .map(this::toMessageView)
                .toList();
    }

    @Transactional
    public MessageView replyToMessage(String workspaceId, String messageId, String authorUserId, String body) {
        var membership = membershipRepository.findByWorkspaceIdAndUserId(workspaceId, authorUserId)
                .orElseThrow(() -> new IllegalArgumentException("User is not a member of workspace " + workspaceId));
        var parent = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Parent message not found: " + messageId));
        MessageEntity saved = messageRepository.save(new MessageEntity(
                "message-" + UUID.randomUUID(),
                membership.getTenantId(),
                workspaceId,
                parent.getChannelId(),
                authorUserId,
                body,
                parent.getMessageId()
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
}
