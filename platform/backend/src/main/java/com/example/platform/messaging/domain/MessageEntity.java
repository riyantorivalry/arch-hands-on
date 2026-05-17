package com.example.platform.messaging.domain;

import com.example.platform.common.domain.AbstractAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "messages", indexes = {
        @Index(name = "idx_message_channel", columnList = "channel_id,created_at")
})
public class MessageEntity extends AbstractAuditableEntity {

    @Id
    @Column(name = "message_id", nullable = false, length = 64)
    private String messageId;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Column(name = "workspace_id", nullable = false, length = 64)
    private String workspaceId;

    @Column(name = "channel_id", nullable = false, length = 64)
    private String channelId;

    @Column(name = "author_user_id", nullable = false, length = 64)
    private String authorUserId;

    @Column(name = "body", nullable = false, length = 4000)
    private String body;

    @Column(name = "parent_message_id", length = 64)
    private String parentMessageId;

    protected MessageEntity() {
    }

    public MessageEntity(
            String messageId,
            String tenantId,
            String workspaceId,
            String channelId,
            String authorUserId,
            String body,
            String parentMessageId
    ) {
        this.messageId = messageId;
        this.tenantId = tenantId;
        this.workspaceId = workspaceId;
        this.channelId = channelId;
        this.authorUserId = authorUserId;
        this.body = body;
        this.parentMessageId = parentMessageId;
    }

    public String getMessageId() {
        return messageId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getWorkspaceId() {
        return workspaceId;
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

    public String getParentMessageId() {
        return parentMessageId;
    }
}
