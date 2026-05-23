Observability stack (Grafana Alloy + Prometheus + Grafana + Loki + Tempo + Pyroscope + Alertmanager + PostgreSQL exporter)

This folder contains a self-contained observability stack you can run locally to collect metrics, logs, traces, and profiles from the backend. It is still a single-node stack, but the defaults mirror production concerns: pinned images, explicit retention, localhost-bound ports, no default `admin/admin` Grafana password, Alloy-based telemetry collection, OTLP trace export, structured log ingestion, trace-derived span metrics, and alert routing through Alertmanager.

Alloy does not replace Prometheus, Loki, Tempo, Grafana, or Alertmanager. Alloy replaces the collector/agent layer: it scrapes metrics, receives OTLP telemetry, tails JSON logs, generates span metrics, and forwards the data to the storage/query backends.

Contents
- `docker-compose.yml` - launches Alloy, Prometheus, Grafana, Loki, Tempo, Pyroscope, Alertmanager, PostgreSQL exporters, and supporting local tools
- `.env.example` - required runtime settings and pinned image versions
- OpenSearch and OpenSearch Dashboards for document/log search
- `prometheus/prometheus.yml` - Prometheus storage, alerting, and minimal self/Alloy scrape configuration
- `prometheus/rules/alert_rules.yml` - availability, SLO, JVM, event, outbox, and PostgreSQL alerts
- `grafana/provisioning` - Grafana provisioning for datasources and dashboards
- `grafana/dashboards` - provisioned production overview, telemetry pipeline, backend, and PostgreSQL dashboards
- `alloy/config.alloy` - Alloy pipelines to scrape metrics, remote-write to Prometheus, receive OTLP traces/metrics, generate span metrics, forward traces to Tempo, tail backend JSON logs, and ship logs to Loki
- `loki/config.yaml` - Loki single-node config with filesystem storage and retention
- `tempo/tempo.yaml` - Tempo single-node trace store
- Pyroscope - single-node profile store for flamegraphs and continuous profiling

Important notes
- The compose file expects the backend to be accessible from the containers as `host.docker.internal:8080` (Windows/Mac). With Alloy as the collector, change `PLATFORM_BACKEND_METRICS_TARGET` in `.env` if Linux needs a different host address such as `172.17.0.1:8080`.
- The PostgreSQL exporters expect the local primary and replica to be accessible as `host.docker.internal:5432` and `host.docker.internal:5433`. If using Linux, update `POSTGRES_PRIMARY_EXPORTER_URI` and `POSTGRES_REPLICA_EXPORTER_URI` in `.env`.
- The backend should be started on port 8080 and write logs to `platform/backend/logs/*.json.log` so Alloy can pick them up.
- Copy `.env.example` to `.env` and set a real `GRAFANA_ADMIN_PASSWORD` before starting the stack.
- This stack is not HA. For production, run Prometheus/Alertmanager/Loki/Tempo in their HA or managed forms, put TLS/auth in front of all UIs/APIs, and back Loki/Tempo with durable object storage.

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
- Prometheus should have `prometheus` and `alloy` under Status -> Targets; Alloy owns the backend, exporter, and observability service scrapes.
- Prometheus queries such as `up{job="platform-backend"}` and `up{job=~"postgres-.*"}` should return series after Alloy has scraped them.
- Grafana should auto-provision the Prometheus, Loki, Tempo, and Pyroscope datasources and import the dashboards `Platform Production Overview`, `Platform Telemetry Pipeline`, `Platform Backend - Observability`, and `Platform PostgreSQL - Connections`.
- Alloy should be pushing JSON logs into Loki; in Grafana Explore you can query Loki with `{job="platform-backend-logs"}`
- Spring Boot traces should flow through Alloy to Tempo when the backend runs with `OTEL_EXPORTER_OTLP_TRACES_ENDPOINT=http://localhost:4318/v1/traces`
- Local/default backend tracing samples every request so one-off requests such as `POST /api/auth/login` appear in Tempo. The `prod` Spring profile defaults to 10% sampling unless `MANAGEMENT_TRACING_SAMPLING_PROBABILITY` is set.
- Trace-derived RED metrics should appear in Prometheus under `traces_spanmetrics_*` after traced traffic reaches Alloy.
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
- If backend metrics don't show, ensure `PLATFORM_BACKEND_METRICS_TARGET` is reachable from the Alloy container. On Windows/Mac `host.docker.internal:8080` works; on Linux you may need to use the host's IP.
- If PostgreSQL metrics don't show, ensure the main `compose.yaml` Postgres services are running and the exporter URIs in `.env` point at reachable host/port pairs.
- If logs aren't ingested, ensure `platform/backend/logs/*.json.log` exist and are mounted.
- If traces don't appear, confirm `management.otlp.tracing.endpoint` resolves to Alloy, tracing sampling is non-zero, and the backend was restarted after changing tracing environment variables.
- If span metric panels are empty, generate HTTP traffic with tracing enabled and query `traces_spanmetrics_calls_total` in Prometheus.
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



