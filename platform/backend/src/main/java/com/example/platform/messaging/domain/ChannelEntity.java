package com.example.platform.messaging.domain;

import com.example.platform.common.domain.AbstractAuditableEntity;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("channels")
public class ChannelEntity extends AbstractAuditableEntity {

    @Id
    @Column("channel_id")
    private String channelId;

    @Column("tenant_id")
    private String tenantId;

    @Column("workspace_id")
    private String workspaceId;

    @Column("name")
    private String name;

    protected ChannelEntity() {
    }

    public ChannelEntity(String channelId, String tenantId, String workspaceId, String name) {
        this.channelId = channelId;
        this.tenantId = tenantId;
        this.workspaceId = workspaceId;
        this.name = name;
    }

    public String getChannelId() {
        return channelId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getWorkspaceId() {
        return workspaceId;
    }

    public String getName() {
        return name;
    }

    @Override
    public Object getId() {
        return channelId;
    }
}
