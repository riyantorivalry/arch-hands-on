create table tasks (
    task_id varchar(64) primary key,
    tenant_id varchar(64) not null,
    workspace_id varchar(64) not null,
    title varchar(200) not null,
    description varchar(4000) not null,
    status varchar(32) not null,
    assignee_user_id varchar(64),
    created_by_user_id varchar(64) not null,
    last_modified_by_user_id varchar(64) not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint fk_tasks_tenant foreign key (tenant_id) references tenants (tenant_id),
    constraint fk_tasks_workspace foreign key (workspace_id) references workspaces (workspace_id),
    constraint fk_tasks_assignee foreign key (assignee_user_id) references users (user_id),
    constraint fk_tasks_creator foreign key (created_by_user_id) references users (user_id),
    constraint fk_tasks_modifier foreign key (last_modified_by_user_id) references users (user_id)
);

create index idx_tasks_workspace on tasks (workspace_id, updated_at);

create table task_comments (
    comment_id varchar(64) primary key,
    task_id varchar(64) not null,
    author_user_id varchar(64) not null,
    body varchar(4000) not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint fk_task_comments_task foreign key (task_id) references tasks (task_id),
    constraint fk_task_comments_author foreign key (author_user_id) references users (user_id)
);

create index idx_task_comments_task on task_comments (task_id, created_at);
