package com.example.platform.messaging.domain;

import com.example.platform.common.domain.DomainEvent;

/**
 * Event published when a message is posted to a channel.
 */
public class MessagePostedEvent extends DomainEvent {
    private final String channelId;
    private final String authorUserId;
    private final String body;

    public MessagePostedEvent(String tenantId, String messageId, String channelId, String authorUserId, String body) {
        super(tenantId, messageId);
        this.channelId = channelId;
        this.authorUserId = authorUserId;
        this.body = body;
    }

    @Override
    public String getEventType() {
        return "MessagePostedEvent";
    }

    public String getChannelId() {
        return channelId;
    }

    public String getAuthorUserId() {
        return authorUserId;
    }

    public String getBody() {
        return body;
    }
}

