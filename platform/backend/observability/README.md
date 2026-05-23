Observability stack (Prometheus + Grafana + Loki + Alloy + Tempo + OpenTelemetry Collector + Pyroscope + Alertmanager + PostgreSQL exporter)

This folder contains a self-contained observability stack you can run locally to collect metrics, logs, traces, and profiles from the backend. It uses an agent-first tracing model: the OpenTelemetry Java agent instruments Spring/JDBC/client libraries and exports OTLP to the collector, while backend code only adds small domain spans through the OpenTelemetry API. JSON logs still flow through Alloy to Loki, and flamegraphs flow to Pyroscope.

Contents
- `docker-compose.yml` - launches Prometheus, Grafana, Loki, Alloy, Tempo, the OpenTelemetry Collector, Pyroscope, Alertmanager, and PostgreSQL exporters
- `.env.example` - required runtime settings and pinned image versions
- `prometheus/prometheus.yml` - Prometheus scrape configuration
- `prometheus/rules/alert_rules.yml` - availability, SLO, JVM, event, outbox, and PostgreSQL alerts
- `grafana/provisioning` - Grafana provisioning for datasources and dashboards
- `grafana/dashboards` - provisioned operational dashboards, including the production overview, telemetry pipeline, and PostgreSQL dashboards
- `alloy/config.alloy` - Alloy pipeline to tail backend JSON logs and ship them to Loki
- `loki/config.yaml` - Loki single-node config with filesystem storage and retention
- `otel/collector-config.yaml` - OTLP receiver and trace exporter to Tempo
- `tempo/tempo.yaml` - Tempo single-node trace store
- `download-opentelemetry-javaagent.ps1` - downloads the OpenTelemetry Java agent into ignored local storage
- `run-backend-with-otel-agent.ps1` - starts the backend with the Java agent and local observability defaults

Important notes
- The compose file expects the backend to be accessible from the containers as `host.docker.internal:8080` (Windows/Mac). If using Linux, change `prometheus.yml` target to `host.docker.internal` alternative or `172.17.0.1:8080` as appropriate.
- The PostgreSQL exporters expect the local primary and replica to be accessible as `host.docker.internal:5432` and `host.docker.internal:5433`. If using Linux, update `POSTGRES_PRIMARY_EXPORTER_URI` and `POSTGRES_REPLICA_EXPORTER_URI` in `.env`.
- The backend should be started on port 8080 and write JSON logs to either `platform/backend/logs/*.json.log` or repo-root `logs/*.json.log`; Alloy tails both paths for local runs launched from different working directories.
- Do not configure Spring Boot's Micrometer OTLP trace exporter for local runs. The Java agent owns trace and OTLP metric export to avoid duplicate HTTP/JDBC spans.
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
- OpenTelemetry Collector OTLP HTTP: http://localhost:4318
- OpenTelemetry Collector OTLP gRPC: localhost:4317
- PostgreSQL exporter primary: http://localhost:9187/metrics
- PostgreSQL exporter replica: http://localhost:9188/metrics

Verify:
- Prometheus should have `platform-backend` under Status -> Targets
- Prometheus should have `postgres-primary` and `postgres-replica` under Status -> Targets after Postgres is running
- Grafana should auto-provision the Prometheus, Loki, Tempo, and Pyroscope datasources and import the dashboards `Platform Production Overview`, `Platform Telemetry Pipeline`, and `Platform PostgreSQL - Connections`
- Alloy should be pushing JSON logs into Loki; in Grafana Explore you can query Loki with `{job="platform-backend-logs"}`
- Java agent traces should flow to Tempo when the backend starts with `-javaagent:observability/agents/opentelemetry-javaagent.jar` and `OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4318`
- Agent OTLP metrics should appear in Prometheus under the `otel-javaagent` scrape job after the collector receives metrics on port 4318 and exposes them on port 8889
- Application/domain child spans should appear under the agent-generated request spans for application/service/facade methods
- Flamegraphs should appear in Pyroscope/Grafana after starting the backend with `PYROSCOPE_AGENT_ENABLED=true`
- Alertmanager should receive active Prometheus alerts from `prometheus/rules/alert_rules.yml`

Backend runtime with the OpenTelemetry Java agent:

```powershell
cd D:\Project\arch-hands-on\platform\backend\observability
.\run-backend-with-otel-agent.ps1
```

Equivalent JVM settings if you run the packaged jar yourself:

```powershell
java `
  -javaagent:observability\agents\opentelemetry-javaagent.jar `
  -Dotel.service.name=platform-backend `
  -Dotel.exporter.otlp.endpoint=http://localhost:4318 `
  -Dotel.exporter.otlp.protocol=http/protobuf `
  -Dotel.traces.exporter=otlp `
  -Dotel.metrics.exporter=otlp `
  -Dotel.logs.exporter=none `
  -jar target\platform-backend-0.0.1-SNAPSHOT.jar
```

Troubleshooting
- If metrics don't show, ensure backend is reachable from the container. On Windows/Mac `host.docker.internal` works; on Linux you may need to use the host's IP.
- If PostgreSQL metrics don't show, ensure the main `compose.yaml` Postgres services are running and the exporter URIs in `.env` point at reachable host/port pairs.
- If logs aren't ingested, ensure `platform/backend/logs/*.json.log` or repo-root `logs/*.json.log` exist and are mounted.
- If traces don't appear, confirm the backend was started with the Java agent, `OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4318`, and the `otel-collector` container is healthy.
- If domain spans don't appear, confirm the Java agent is attached. The backend uses the OpenTelemetry API only; without the agent, custom spans are intentionally no-op.
- If profile data does not appear, confirm Pyroscope is running and the backend was started with `PYROSCOPE_AGENT_ENABLED=true`.
- To re-create the stack:

```powershell
docker compose down -v
docker compose up -d
```

Optional additions
- Replace the placeholder Alertmanager receivers with PagerDuty, Opsgenie, Slack, email, or webhook receivers for real notifications.
- Configure remote storage: Prometheus remote write, Loki object storage, and Tempo object storage.
- Add authentication/TLS in front of Prometheus, Alertmanager, Loki, Tempo, Alloy, and the OTel Collector before exposing them outside a trusted network.
- Back up Grafana and Prometheus volumes or provision all dashboards, folders, datasources, and alert rules as code.



