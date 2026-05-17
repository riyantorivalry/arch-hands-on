create table workspace_settings (
    workspace_id varchar(64) primary key,
    tenant_id varchar(64) not null,
    default_document_status varchar(32) not null,
    task_auto_assign_enabled boolean not null,
    message_retention_days integer not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint fk_workspace_settings_workspace foreign key (workspace_id) references workspaces (workspace_id),
    constraint fk_workspace_settings_tenant foreign key (tenant_id) references tenants (tenant_id),
    constraint chk_workspace_settings_retention check (message_retention_days > 0)
);

create index idx_workspace_settings_tenant on workspace_settings (tenant_id);
