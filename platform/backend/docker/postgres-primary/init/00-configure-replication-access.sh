#!/bin/sh
set -eu

cat >> "$PGDATA/pg_hba.conf" <<'EOF'
host replication replicator all scram-sha-256
EOF
