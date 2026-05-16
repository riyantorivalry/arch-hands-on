CREATE ROLE replicator WITH REPLICATION LOGIN PASSWORD 'replicator_password';
CREATE ROLE platform_readonly WITH LOGIN PASSWORD 'platform_readonly';

GRANT CONNECT ON DATABASE platform TO platform_readonly;

\connect platform

GRANT USAGE ON SCHEMA public TO platform_readonly;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO platform_readonly;
GRANT SELECT ON ALL SEQUENCES IN SCHEMA public TO platform_readonly;

ALTER DEFAULT PRIVILEGES FOR USER platform IN SCHEMA public
    GRANT SELECT ON TABLES TO platform_readonly;

ALTER DEFAULT PRIVILEGES FOR USER platform IN SCHEMA public
    GRANT SELECT ON SEQUENCES TO platform_readonly;
