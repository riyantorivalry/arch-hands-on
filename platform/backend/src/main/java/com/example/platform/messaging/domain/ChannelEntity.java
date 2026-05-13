package com.example.platform.messaging.domain;

import com.example.platform.common.domain.AbstractAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "channels", indexes = {
        @Index(name = "idx_channel_workspace", columnList = "workspace_id")
})
public class ChannelEntity extends AbstractAuditableEntity {

    @Id
    @Column(name = "channel_id", nullable = false, length = 64)
    private String channelId;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Column(name = "workspace_id", nullable = false, length = 64)
    private String workspaceId;

    @Column(name = "name", nullable = false, length = 120)
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
}
