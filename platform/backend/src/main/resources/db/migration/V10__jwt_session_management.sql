alter table user_sessions add column refresh_token_hash varchar(128);
alter table user_sessions add column client_id varchar(120) default 'web' not null;
alter table user_sessions add column client_type varchar(32) default 'WEB' not null;
alter table user_sessions add column issued_at timestamp with time zone;
alter table user_sessions add column last_seen_at timestamp with time zone;
alter table user_sessions add column revoked_at timestamp with time zone;

update user_sessions
set refresh_token_hash = session_token,
    issued_at = created_at
where refresh_token_hash is null;

alter table user_sessions alter column refresh_token_hash set not null;
alter table user_sessions alter column issued_at set not null;

create unique index idx_sessions_refresh_token_hash on user_sessions (refresh_token_hash);
create index idx_sessions_status_expires_at on user_sessions (status, expires_at);
create index idx_sessions_client on user_sessions (client_id, client_type);
