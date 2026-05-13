create table user_sessions (
    session_token varchar(128) primary key,
    tenant_id varchar(64) not null,
    workspace_id varchar(64) not null,
    user_id varchar(64) not null,
    status varchar(32) not null,
    expires_at timestamp with time zone not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint fk_sessions_tenant foreign key (tenant_id) references tenants (tenant_id),
    constraint fk_sessions_workspace foreign key (workspace_id) references workspaces (workspace_id),
    constraint fk_sessions_user foreign key (user_id) references users (user_id)
);

create index idx_sessions_user_workspace on user_sessions (user_id, workspace_id);
