Observability stack (Prometheus + Grafana + Loki + Alloy + Tempo + Pyroscope + Alertmanager + PostgreSQL exporter)

This folder contains a self-contained observability stack you can run locally to collect metrics, logs and traces from the backend. It is still a single-node development stack, but the defaults now mirror production concerns: pinned images, explicit retention, no default `admin/admin` Grafana password, OTLP trace export, log collection with supported Grafana Alloy, and alert routing through Alertmanager.

Contents
- `docker-compose.yml` - launches Prometheus, Grafana, Loki, Alloy, Tempo, Pyroscope, Alertmanager, PostgreSQL exporters, and supporting local tools
- `.env.example` - required runtime settings and pinned image versions
- OpenSearch and OpenSearch Dashboards for document/log search
- `prometheus/prometheus.yml` - Prometheus scrape configuration
- `prometheus/rules/alert_rules.yml` - availability, SLO, JVM, event, outbox, and PostgreSQL alerts
- `grafana/provisioning` - Grafana provisioning for datasources and dashboards
- `grafana/dashboards` - example dashboard JSON
- `alloy/config.alloy` - Alloy pipelines to receive OTLP traces, forward traces to Tempo, tail backend JSON logs, and ship logs to Loki
- `loki/config.yaml` - Loki single-node config with filesystem storage and retention
- `tempo/tempo.yaml` - Tempo single-node trace store
- Pyroscope - single-node profile store for flamegraphs and continuous profiling

Important notes
- The compose file expects the backend to be accessible from the containers as `host.docker.internal:8080` (Windows/Mac). If using Linux, change `prometheus.yml` target to `host.docker.internal` alternative or `172.17.0.1:8080` as appropriate.
- The PostgreSQL exporters expect the local primary and replica to be accessible as `host.docker.internal:5432` and `host.docker.internal:5433`. If using Linux, update `POSTGRES_PRIMARY_EXPORTER_URI` and `POSTGRES_REPLICA_EXPORTER_URI` in `.env`.
- The backend should be started on port 8080 and write logs to `platform/backend/logs/*.json.log` so Alloy can pick them up.
- Copy `.env.example` to `.env` and set a real `GRAFANA_ADMIN_PASSWORD` before starting the stack.
- This stack is not HA. For production, run Prometheus/Alertmanager/Loki/Tempo in their HA or managed forms and back their storage with durable remote object storage.

Quick start (PowerShell)

```powershell
cd D:\Project\arch-hands-on\platform\backend\observability
Copy-Item .env.example .env
# Edit .env and replace GRAFANA_ADMIN_PASSWORD before starting.
docker compose up -d
```

Open the following UIs:
- Grafana: http://localhost:3000
- Prometheus: http://localhost:9090
- Loki (API): http://localhost:3100
- Tempo (API): http://localhost:3200
- Pyroscope: http://localhost:4040
- Alertmanager: http://localhost:9093
- Alloy: http://localhost:12345
- Alloy OTLP HTTP: http://localhost:4318
- Alloy OTLP gRPC: localhost:4317
- PostgreSQL exporter primary: http://localhost:9187/metrics
- PostgreSQL exporter replica: http://localhost:9188/metrics
- OpenSearch Dashboards: http://localhost:5601

Verify:
- Prometheus should have `platform-backend` under Status -> Targets
- Prometheus should have `postgres-primary` and `postgres-replica` under Status -> Targets after Postgres is running
- Grafana should auto-provision the Prometheus, Loki, Tempo, and Pyroscope datasources and import the dashboards `Platform Backend - Observability` and `Platform PostgreSQL - Connections`
- Alloy should be pushing JSON logs into Loki; in Grafana Explore you can query Loki with `{job="platform-backend-logs"}`
- Spring Boot traces should flow through Alloy to Tempo when the backend runs with `OTEL_EXPORTER_OTLP_TRACES_ENDPOINT=http://localhost:4318/v1/traces`
- JDBC timings should appear in Prometheus as `database_query_time_seconds_*` after API requests hit PostgreSQL
- Flamegraphs should appear in Pyroscope/Grafana after starting the backend with profiling enabled
- Alertmanager should receive active Prometheus alerts from `prometheus/rules/alert_rules.yml`

Backend runtime settings for full local observability:

```powershell
$env:MANAGEMENT_TRACING_SAMPLING_PROBABILITY="1.0"
$env:OTEL_EXPORTER_OTLP_TRACES_ENDPOINT="http://localhost:4318/v1/traces"
$env:PYROSCOPE_AGENT_ENABLED="true"
$env:PYROSCOPE_SERVER_ADDRESS="http://localhost:4040"
$env:PYROSCOPE_APPLICATION_NAME="platform-backend"
```

Then run the IntelliJ `RUN` application configuration, or use the equivalent Maven command from `platform/backend`:

```powershell
mvn spring-boot:run "-Dspring-boot.run.jvmArguments=-Dspring.profiles.active="
```

The Pyroscope Java profiler depends on native async-profiler support. If it cannot start on the host OS, the backend logs a warning and continues running; use WSL/Linux or a Linux container for reliable local flamegraphs.

Troubleshooting
- If metrics don't show, ensure backend is reachable from the container. On Windows/Mac `host.docker.internal` works; on Linux you may need to use the host's IP.
- If PostgreSQL metrics don't show, ensure the main `compose.yaml` Postgres services are running and the exporter URIs in `.env` point at reachable host/port pairs.
- If logs aren't ingested, ensure `platform/backend/logs/*.json.log` exist and are mounted.
- If traces don't appear, confirm `management.otlp.tracing.endpoint` resolves to Alloy and tracing sampling is non-zero.
- If profile data does not appear, confirm Pyroscope is running, `PYROSCOPE_AGENT_ENABLED=true` is set for the backend process, and the Java profiler supports the current host OS.
- To re-create the stack:

```powershell
docker compose down -v
docker compose up -d
```

Optional additions
- Replace the placeholder Alertmanager receivers with PagerDuty, Opsgenie, Slack, email, or webhook receivers for real notifications.
- Configure remote storage: Prometheus remote write, Loki object storage, and Tempo object storage.
- Add authentication/TLS in front of Prometheus, Alertmanager, Loki, Tempo, and Alloy before exposing them outside a trusted network.
- Back up Grafana and Prometheus volumes or provision all dashboards, folders, datasources, and alert rules as code.



