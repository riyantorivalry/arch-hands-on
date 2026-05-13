package com.example.platform.messaging.application;

import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class MessagingFacade {

    public ChannelView createChannel(String workspaceId, String channelName) {
        return new ChannelView("channel-" + channelName.toLowerCase().replace(" ", "-"), workspaceId, channelName);
    }

    public List<ChannelView> listChannels(String workspaceId) {
        return List.of(new ChannelView("channel-general", workspaceId, "general"));
    }

    public MessageView postMessage(String channelId, String authorUserId, String body) {
        return new MessageView("message-1", channelId, authorUserId, body, null);
    }

    public List<MessageView> listMessages(String channelId) {
        return List.of(new MessageView("message-1", channelId, "user-dev", "Hello from scaffold", null));
    }

    public MessageView replyToMessage(String messageId, String authorUserId, String body) {
        return new MessageView("message-reply-1", "channel-general", authorUserId, body, messageId);
    }

    public record ChannelView(String channelId, String workspaceId, String name) {
    }

    public record MessageView(String messageId, String channelId, String authorUserId, String body, String parentMessageId) {
    }
}
