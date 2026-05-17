create table authorization_policy_rules (
    rule_id varchar(96) primary key,
    policy_id varchar(96) not null,
    effect varchar(16) not null,
    role_name varchar(32) not null,
    action varchar(120) not null,
    resource_type varchar(64) not null,
    condition_type varchar(64) not null,
    priority integer not null,
    enabled boolean not null,
    created_at timestamp with time zone not null,
    constraint chk_authorization_policy_rules_effect check (effect in ('ALLOW', 'DENY')),
    constraint chk_authorization_policy_rules_priority check (priority > 0)
);

create index idx_authorization_policy_rules_enabled_priority
    on authorization_policy_rules (enabled, priority);

insert into authorization_policy_rules (
    rule_id,
    policy_id,
    effect,
    role_name,
    action,
    resource_type,
    condition_type,
    priority,
    enabled,
    created_at
) values
    ('db-policy-owner-all', 'db-workspace-policy-v1', 'ALLOW', 'OWNER', '*', '*', 'NONE', 10, true, current_timestamp),
    ('db-policy-admin-all', 'db-workspace-policy-v1', 'ALLOW', 'ADMIN', '*', '*', 'NONE', 10, true, current_timestamp),
    ('db-policy-member-high-risk-deny', 'db-workspace-policy-v1', 'DENY', 'MEMBER', '*', '*', 'HIGH_RISK', 50, true, current_timestamp),
    ('db-policy-member-workspace-read', 'db-workspace-policy-v1', 'ALLOW', 'MEMBER', 'workspace:read', 'workspace', 'NONE', 100, true, current_timestamp),
    ('db-policy-member-document-read', 'db-workspace-policy-v1', 'ALLOW', 'MEMBER', 'document:read', 'document', 'NONE', 100, true, current_timestamp),
    ('db-policy-member-document-search', 'db-workspace-policy-v1', 'ALLOW', 'MEMBER', 'document:search', 'document', 'NONE', 100, true, current_timestamp),
    ('db-policy-member-document-create', 'db-workspace-policy-v1', 'ALLOW', 'MEMBER', 'document:create', 'document', 'NONE', 100, true, current_timestamp),
    ('db-policy-member-task-read', 'db-workspace-policy-v1', 'ALLOW', 'MEMBER', 'task:read', 'task', 'NONE', 100, true, current_timestamp),
    ('db-policy-member-task-create', 'db-workspace-policy-v1', 'ALLOW', 'MEMBER', 'task:create', 'task', 'NONE', 100, true, current_timestamp),
    ('db-policy-member-message-read', 'db-workspace-policy-v1', 'ALLOW', 'MEMBER', 'message:read', 'message', 'NONE', 100, true, current_timestamp),
    ('db-policy-member-message-post', 'db-workspace-policy-v1', 'ALLOW', 'MEMBER', 'message:post', 'message', 'NONE', 100, true, current_timestamp),
    ('db-policy-member-document-owner-update', 'db-workspace-policy-v1', 'ALLOW', 'MEMBER', 'document:update', 'document', 'RESOURCE_OWNER', 120, true, current_timestamp),
    ('db-policy-member-task-owner-assignee-update', 'db-workspace-policy-v1', 'ALLOW', 'MEMBER', 'task:update', 'task', 'RESOURCE_OWNER_OR_ASSIGNEE', 120, true, current_timestamp);
