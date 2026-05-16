# PostgreSQL Primary-Replica Setup

This project supports routing write transactions to a PostgreSQL primary and read-only transactions to a PostgreSQL replica.

Application routing behavior:

- `@Transactional` uses the primary datasource.
- `@Transactional(readOnly = true)` uses the replica datasource.
- If no replica JDBC URL is configured, read-only transactions fall back to the primary.

## Local Docker Compose Setup

The backend Compose file starts:

- `postgres` on host port `5432` as the primary database
- `postgres-replica` on host port `5433` as a hot standby read replica

Start both databases:

```bash
cd platform/backend
docker compose up -d postgres postgres-replica
```

Check status:

```bash
docker compose ps postgres postgres-replica
```

## Application Configuration

Use the primary for writes:

```yaml
spring:
  datasource:
    master:
      url: jdbc:postgresql://localhost:5432/platform
      username: platform
      password: platform
```

Use the replica for read-only transactions:

```yaml
spring:
  datasource:
    replica:
      url: jdbc:postgresql://localhost:5433/platform
      username: platform_readonly
      password: platform_readonly
      hikari:
        read-only: true
```

The `platform_readonly` role is created by:

```text
platform/backend/docker/postgres-primary/init/01-create-replication-and-readonly-users.sql
```

That role can connect to the `platform` database and select from tables and sequences in the `public` schema. It is intended for the application replica datasource only.

Replication access is configured by:

```text
platform/backend/docker/postgres-primary/init/00-configure-replication-access.sh
```

The script appends this rule to the primary `pg_hba.conf` during first database initialization:

```text
host replication replicator all scram-sha-256
```

## How Replication Works

The primary container starts PostgreSQL with streaming replication settings:

```text
wal_level=replica
max_wal_senders=10
max_replication_slots=10
hot_standby=on
```

On first startup, the replica container:

1. Waits for the primary to become healthy.
2. Runs `pg_basebackup` against the primary using the `replicator` role.
3. Writes standby configuration with `pg_basebackup -R`.
4. Sets replica data directory permissions to `0700`.
5. Starts PostgreSQL in standby mode.

The replica data volume is separate from the primary data volume.

## Verify Replication

Create test data on the primary:

```bash
docker compose exec postgres psql -U platform -d platform -c "create table if not exists replica_check (id text primary key);"
docker compose exec postgres psql -U platform -d platform -c "insert into replica_check (id) values ('ok') on conflict do nothing;"
```

Read it from the replica:

```bash
docker compose exec postgres-replica psql -U platform_readonly -d platform -c "select * from replica_check;"
```

Confirm the replica rejects writes:

```bash
docker compose exec postgres-replica psql -U platform_readonly -d platform -c "insert into replica_check (id) values ('should-fail');"
```

Expected result: PostgreSQL rejects the write because the server is in recovery/read-only standby mode.

Check replication state from the primary:

```bash
docker compose exec postgres psql -U platform -d platform -c "select application_name, state, sync_state from pg_stat_replication;"
```

Check standby state from the replica:

```bash
docker compose exec postgres-replica psql -U platform_readonly -d platform -c "select pg_is_in_recovery();"
```

Expected result:

```text
pg_is_in_recovery
-------------------
t
```

## Reset Local Replication

If the replica fails to initialize or you change replication credentials, reset the database volumes:

```bash
cd platform/backend
docker compose down -v
docker compose up -d postgres postgres-replica
```

This deletes local database data.

If only the replica data directory is broken, keep the primary and reset only the replica volume:

```bash
cd platform/backend
docker compose stop postgres-replica
docker volume rm backend_postgres-replica-data
docker compose up -d postgres-replica
```

If you need to keep the existing primary data volume, patch `pg_hba.conf` in place instead:

```bash
docker compose exec postgres sh -c 'printf "%s\n" "host replication replicator all scram-sha-256" >> "$PGDATA/pg_hba.conf"'
docker compose exec postgres psql -U platform -d platform -c "select pg_reload_conf();"
docker compose up -d postgres-replica
```

## Production Notes

For production, do not use the local Compose credentials. Use managed secret storage and rotate:

- primary application user password
- replica read-only user password
- replication user password

Use private networking between primary and replica. Do not expose replication ports publicly.

This setup is asynchronous streaming replication. Reads from the replica may briefly lag behind writes on the primary. Flows that require read-after-write consistency should read from the primary or retry until the expected version is visible.
