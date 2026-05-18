# k6 Benchmarks

This folder contains separate k6 workloads for two different questions:

- `comparison-endpoints.js` benchmarks architecture comparison endpoints and strategy-specific APIs.
- `collaboration-apis.js` benchmarks normal product collaboration APIs only. It does not call `/api/benchmarks/*`.

## Comparison Endpoint Run

```bash
k6 run docs/benchmarks/k6/comparison-endpoints.js
```

Defaults:

- `BASE_URL=http://localhost:8080`
- `SUITE=all`
- `VUS=10`
- `RAMP_UP=30s`
- `HOLD=1m`
- `RAMP_DOWN=15s`
- `CACHE_STRATEGIES=caffeine`
- `ANALYTICS_VERSIONS=v1`
- `SEARCH_VERSIONS=v1,v2,v3`

The default avoids external infrastructure assumptions beyond the local backend and database. Redis, Memcached, and MongoDB comparison runs should be enabled only when those services are configured and healthy.

## Targeted Runs

Run only one comparison track:

```bash
k6 run -e SUITE=auth docs/benchmarks/k6/comparison-endpoints.js
k6 run -e SUITE=cache docs/benchmarks/k6/comparison-endpoints.js
k6 run -e SUITE=rate-limit docs/benchmarks/k6/comparison-endpoints.js
k6 run -e SUITE=realtime docs/benchmarks/k6/comparison-endpoints.js
k6 run -e SUITE=analytics docs/benchmarks/k6/comparison-endpoints.js
k6 run -e SUITE=search docs/benchmarks/k6/comparison-endpoints.js
```

Run multiple tracks:

```bash
k6 run -e SUITE=auth,rate-limit,cache docs/benchmarks/k6/comparison-endpoints.js
```

Run against another backend:

```bash
k6 run -e BASE_URL=https://api.example.test docs/benchmarks/k6/comparison-endpoints.js
```

## External Strategy Runs

Cache strategies:

```bash
k6 run -e SUITE=cache -e CACHE_STRATEGIES=caffeine,redis,memcached docs/benchmarks/k6/comparison-endpoints.js
```

Analytics stores:

```bash
k6 run -e SUITE=analytics -e ANALYTICS_VERSIONS=v1,v2 docs/benchmarks/k6/comparison-endpoints.js
```

Authorization engines:

```bash
k6 run -e SUITE=auth -e AUTH_ENGINES=in-code,opa-local,casbin-local,db-policy docs/benchmarks/k6/comparison-endpoints.js
```

Rate limit algorithms:

```bash
k6 run -e SUITE=rate-limit -e RATE_LIMIT_ALGORITHMS=fixed-window,sliding-window,token-bucket docs/benchmarks/k6/comparison-endpoints.js
```

## Load Shape

Tune the load with:

```bash
k6 run \
  -e VUS=25 \
  -e RAMP_UP=1m \
  -e HOLD=5m \
  -e RAMP_DOWN=30s \
  -e P95_MS=1000 \
  docs/benchmarks/k6/comparison-endpoints.js
```

The script sets two default thresholds:

- `http_req_failed < 1%`
- `http_req_duration p95 < 750ms`

## Collaboration API Run

`collaboration-apis.js` creates a unique tenant/workspace during `setup()`, logs in as an owner and a member, seeds a channel, document, and task, then exercises normal collaboration APIs:

- identity and workspace reads
- channel and message reads/writes
- document reads/search/comments/updates
- task reads/comments/updates

Default mixed read/write run:

```bash
k6 run docs/benchmarks/k6/collaboration-apis.js
```

Read-only run against the seeded workspace:

```bash
k6 run -e READ_ONLY=true docs/benchmarks/k6/collaboration-apis.js
```

Tune load shape:

```bash
k6 run \
  -e VUS=25 \
  -e RAMP_UP=1m \
  -e HOLD=5m \
  -e RAMP_DOWN=30s \
  -e P95_MS=1000 \
  docs/benchmarks/k6/collaboration-apis.js
```
