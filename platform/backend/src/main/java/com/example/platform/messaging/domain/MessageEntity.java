package com.example.platform.messaging.domain;

import com.example.platform.common.domain.AbstractAuditableEntity;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("messages")
public class MessageEntity extends AbstractAuditableEntity {

    @Id
    @Column("message_id")
    private String messageId;

    @Column("tenant_id")
    private String tenantId;

    @Column("workspace_id")
    private String workspaceId;

    @Column("channel_id")
    private String channelId;

    @Column("author_user_id")
    private String authorUserId;

    @Column("body")
    private String body;

    @Column("parent_message_id")
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

    @Override
    public Object getId() {
        return messageId;
    }
}
