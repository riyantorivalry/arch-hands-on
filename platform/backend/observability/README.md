Observability stack (Prometheus + Grafana + Loki + Jaeger)

This folder contains a self-contained observability stack you can run locally to collect metrics, logs and traces from the backend.

Contents
- `docker-compose.yml` - launches Prometheus, Grafana, Loki, Promtail and Jaeger
- `prometheus/prometheus.yml` - Prometheus scrape configuration
- `grafana/provisioning` - Grafana provisioning for datasources and dashboards
- `grafana/dashboards` - example dashboard JSON
- `loki/promtail-config.yaml` - promtail config to tail backend JSON logs

Important notes
- The compose file expects the backend to be accessible from the containers as `host.docker.internal:8080` (Windows/Mac). If using Linux, change `prometheus.yml` target to `host.docker.internal` alternative or `172.17.0.1:8080` as appropriate.
- The backend should be started on port 8080 and write logs to `platform/backend/logs/*.json.log` so `promtail` can pick them up.

Quick start (PowerShell)

```powershell
cd D:\Project\arch-hands-on\platform\backend\observability
docker compose up -d
```

Open the following UIs:
- Grafana: http://localhost:3000  (user: admin, password: admin)
- Prometheus: http://localhost:9090
- Loki (API): http://localhost:3100
- Jaeger UI: http://localhost:16686

Verify:
- Prometheus should have `platform-backend` under Status -> Targets
- Grafana should auto-provision the Prometheus and Loki datasources and import the dashboard `Platform Backend - Observability`
- Promtail should be pushing JSON logs into Loki; in Grafana explore you can query Loki with `{job="platform-backend-logs"}`

Troubleshooting
- If metrics don't show, ensure backend is reachable from the container. On Windows/Mac `host.docker.internal` works; on Linux you may need to use the host's IP.
- If logs aren't ingested, ensure `platform/backend/logs/*.json.log` exist and are mounted.
- To re-create the stack:

```powershell
docker compose down -v
docker compose up -d
```

Optional additions
- Alertmanager is included (port 9093) and Prometheus alert rules are provided at `prometheus/rules/alert_rules.yml`.
- Grafana dashboards are persisted in `grafana/dashboards` and auto-provisioned via `grafana/provisioning` (extended dashboard included).
- Add persistent volumes and backup for Grafana and Prometheus data
- Add Tempo or OTEL collector for richer tracing pipelines



