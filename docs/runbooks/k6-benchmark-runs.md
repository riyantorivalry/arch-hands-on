# k6 Benchmark Runs

## Purpose

Use this runbook to execute repeatable k6 load tests against the backend.

The repository has two k6 workloads:

- [comparison-endpoints.js](../benchmarks/k6/comparison-endpoints.js) for architecture comparison endpoints and strategy-specific APIs
- [collaboration-apis.js](../benchmarks/k6/collaboration-apis.js) for normal collaboration product APIs only

Use the collaboration workload when measuring user-facing behavior. Use the comparison workload when comparing implementation strategies such as cache backends, rate limit algorithms, authorization engines, analytics stores, or search versions.

## Prerequisites

- Backend is running and reachable from the machine running k6.
- PostgreSQL migrations have completed.
- k6 is installed and available on `PATH`.
- For default local runs, no Redis, Memcached, MongoDB, or OpenSearch dependency is required beyond what the backend itself needs to start.
- For external strategy runs, the target infrastructure must be configured and healthy before the test starts.

Quick verification:

```bash
k6 version
curl http://localhost:8080/actuator/health
```

On Windows, install k6 through your normal package manager, for example `winget` or Chocolatey. Docker can also be used if the container can reach the backend URL.

## Safety Notes

- These scripts create tenants, users, memberships, channels, documents, tasks, comments, messages, and analytics/search events.
- Run against disposable local or staging data unless the target environment explicitly allows synthetic load.
- Do not run write-heavy tests against production.
- Use `READ_ONLY=true` for the collaboration script when you only want read traffic after setup data is created.
- Increase load gradually; start with the default profile before raising VUs or hold duration.

## Environment Variables

Common variables:

| Variable | Default | Meaning |
|----------|---------|---------|
| `BASE_URL` | `http://localhost:8080` | Backend base URL |
| `VUS` | `10` | Target virtual users |
| `RAMP_UP` | `30s` | Ramp-up duration |
| `HOLD` | `1m` or `2m` | Steady-state duration, depending on script |
| `RAMP_DOWN` | `15s` or `30s` | Ramp-down duration, depending on script |
| `P95_MS` | `750` or `1000` | Default p95 threshold, depending on script |
| `THINK_TIME_SECONDS` | `0.1` or `0.2` | Sleep time between iterations |
| `HTML_REPORT` | script-specific file in the current working directory | HTML report output path |
| `JSON_REPORT` | unset | Optional raw k6 summary JSON output path |

Comparison script variables:

| Variable | Default | Meaning |
|----------|---------|---------|
| `SUITE` | `all` | Comma-separated track list: `auth`, `cache`, `rate-limit`, `realtime`, `analytics`, `search` |
| `CACHE_STRATEGIES` | `caffeine` | Cache strategies to invoke |
| `RATE_LIMIT_ALGORITHMS` | `fixed-window,sliding-window,token-bucket` | Rate limit algorithms to invoke |
| `AUTH_ENGINES` | `in-code,opa-local,casbin-local,db-policy` | Authorization engines to invoke |
| `ANALYTICS_VERSIONS` | `v1` | Analytics API versions to invoke |
| `SEARCH_VERSIONS` | `v1,v2,v3` | Document search API versions to invoke |

Collaboration script variables:

| Variable | Default | Meaning |
|----------|---------|---------|
| `READ_ONLY` | `false` | When `true`, run read paths only after setup |

## Normal Collaboration API Run

Run the user-facing collaboration workflow:

```bash
k6 run docs/benchmarks/k6/collaboration-apis.js
```

Run read-only traffic after setup:

```bash
k6 run -e READ_ONLY=true docs/benchmarks/k6/collaboration-apis.js
```

Run a longer local test:

```bash
k6 run \
  -e VUS=25 \
  -e RAMP_UP=1m \
  -e HOLD=5m \
  -e RAMP_DOWN=30s \
  -e P95_MS=1000 \
  docs/benchmarks/k6/collaboration-apis.js
```

Primary questions this workload answers:

- How does the normal collaboration flow behave under mixed reads and writes?
- Which product endpoints are slow under regular user traffic?
- Does the backend sustain workspace, channel, message, document, and task traffic without comparison endpoint noise?

## Architecture Comparison Run

Run all default comparison tracks:

```bash
k6 run docs/benchmarks/k6/comparison-endpoints.js
```

Run one track:

```bash
k6 run -e SUITE=auth docs/benchmarks/k6/comparison-endpoints.js
k6 run -e SUITE=cache docs/benchmarks/k6/comparison-endpoints.js
k6 run -e SUITE=rate-limit docs/benchmarks/k6/comparison-endpoints.js
k6 run -e SUITE=realtime docs/benchmarks/k6/comparison-endpoints.js
k6 run -e SUITE=analytics docs/benchmarks/k6/comparison-endpoints.js
k6 run -e SUITE=search docs/benchmarks/k6/comparison-endpoints.js
```

Run external cache strategies only after Redis and Memcached are configured:

```bash
k6 run \
  -e SUITE=cache \
  -e CACHE_STRATEGIES=caffeine,redis,memcached \
  docs/benchmarks/k6/comparison-endpoints.js
```

Run MongoDB analytics comparison only after MongoDB is configured:

```bash
k6 run \
  -e SUITE=analytics \
  -e ANALYTICS_VERSIONS=v1,v2 \
  docs/benchmarks/k6/comparison-endpoints.js
```

Primary questions this workload answers:

- Which implementation strategy has better latency under the same request shape?
- Does one strategy fail or degrade faster under load?
- Which strategy adds operational dependencies or failure modes?

## Capturing Results

The scripts generate a production-ready HTML summary in the current working directory by default:

- `k6-collaboration-apis-report.html`
- `k6-comparison-endpoints-report.html`

Each report includes an executive summary, configured run metadata, threshold status, and a per-API response-time table with request count, failure rate, average, min, median, p90, p95, p99, and max latency.

Use `HTML_REPORT` when you want a unique archived report file per run. Create the target directory first because k6 does not create parent directories from `handleSummary`:

```bash
mkdir -p docs/benchmarks/results

k6 run \
  -e HTML_REPORT=docs/benchmarks/results/collaboration-apis-2026-05-23.html \
  docs/benchmarks/k6/collaboration-apis.js
```

PowerShell equivalent:

```powershell
New-Item -ItemType Directory -Force docs/benchmarks/results
```

Save the raw k6 summary JSON as well:

```bash
k6 run \
  -e HTML_REPORT=docs/benchmarks/results/collaboration-apis-report.html \
  -e JSON_REPORT=docs/benchmarks/results/collaboration-apis-summary.json \
  docs/benchmarks/k6/collaboration-apis.js
```

For a benchmark report, copy [../benchmarks/BENCHMARK_TEMPLATE.md](../benchmarks/BENCHMARK_TEMPLATE.md) to a new file:

```text
docs/benchmarks/BENCHMARK-YYYY-MM-DD-[topic].md
```

Record at minimum:

- script path and git commit
- backend configuration
- database and external service configuration
- load shape
- p50, p95, p99 latency
- request rate
- failure rate
- CPU and memory observations
- any threshold failures

## Interpreting Failures

| Symptom | Likely Cause | Action |
|---------|--------------|--------|
| Setup fails on tenant creation | Backend unavailable or DB migration issue | Check `/actuator/health` and backend logs |
| Login fails during setup | Tenant/workspace bootstrap failed | Inspect the tenant creation response in the k6 error |
| High `http_req_failed` | Endpoint errors, auth failures, or external strategy unavailable | Run the smallest targeted suite and inspect backend logs |
| Cache suite fails for Redis/Memcached | External cache not configured or unavailable | Run `CACHE_STRATEGIES=caffeine` first, then verify external service config |
| Analytics `v2` fails | MongoDB unavailable or auth mismatch | Verify MongoDB connection settings |
| Search `v3` is slower but passes | OpenSearch fallback or network dependency | Check backend logs for OpenSearch fallback warnings |
| p95 threshold fails | Backend saturated or threshold unrealistic for environment | Capture CPU, memory, DB metrics, then rerun with lower VUs |

## Cleanup

The scripts do not delete generated tenants or workspace data. For local development this is usually acceptable. For staging, prefer a disposable database or clean up synthetic tenants by database reset after the run.

Do not manually delete rows from a shared environment unless the owning team approves the cleanup plan.
