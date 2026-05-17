# Backend

This is the Phase 1 Java modular monolith scaffold with comprehensive observability.

## Stack

- Java 17
- Spring Boot 3.4.0
- Spring Web
- Spring Data JPA
- PostgreSQL
- Spring Boot Actuator
- Micrometer Metrics & Tracing
- Logback with JSON formatting
- Micrometer Tracing with OpenTelemetry OTLP export

## Module layout

Each business module follows the package pattern defined in the architecture docs:

```text
com.example.platform.<module>
  api
  application
  domain
  infrastructure
```

Current modules:

- `identityaccess`
- `tenantmanagement`
- `messaging`
- `documents`
- `tasks`
- `realtime`

## Current scope

This scaffold establishes:

- one deployable Spring Boot app
- tenant-aware request context plumbing
- explicit module packages
- placeholder application services and controllers
- health and platform info endpoints

Feature persistence and domain rules should be added module by module from the Phase 1 contracts.

## Realtime Delivery Comparison

Realtime updates use one shared versioned event envelope across the comparison endpoints:

```text
GET /api/v1/workspaces/{workspaceId}/events?since={cursor}
GET /api/v2/workspaces/{workspaceId}/events/stream?since={cursor}
WS  /ws/v3/realtime
```

The polling response returns `nextCursor`; pass that value as `since` on the next request. The SSE stream uses the event version as the SSE event ID. The WebSocket endpoint accepts subscription messages like:

```json
{"action":"subscribe","workspaceId":"workspace-engineering"}
```

## Postgres Master-Replica Routing

The backend supports read/write datasource routing:

- write transactions (`@Transactional`) -> `spring.datasource.master`
- read-only transactions (`@Transactional(readOnly = true)`) -> `spring.datasource.replica`
- if replica is not configured, reads automatically fall back to master

Local primary-replica setup is documented in [PostgreSQL primary-replica setup](../../docs/runbooks/postgres-read-replica.md).

Example config:

```yaml
spring:
  datasource:
    master:
      url: jdbc:postgresql://postgres-master:5432/platform
      username: platform_rw
      password: ***
    replica:
      url: jdbc:postgresql://postgres-replica:5432/platform
      username: platform_ro
      password: ***
      hikari:
        read-only: true
```

## MongoDB Analytics

The local Docker Compose MongoDB instance enables authentication. The default Spring
configuration connects with the same credentials:

```text
SPRING_DATA_MONGODB_USERNAME=admin
SPRING_DATA_MONGODB_PASSWORD=secretpassword
SPRING_DATA_MONGODB_AUTHENTICATION_DATABASE=admin
```

Override those variables when using a different MongoDB instance.

## Analytics Storage Comparison

Analytics events are exposed through versioned comparison endpoints:

```text
GET /api/v1/tenants/{tenantId}/analytics/events
GET /api/v2/tenants/{tenantId}/analytics/events
```

`/api/v1` reads from PostgreSQL using the `analytics_events.event_data` JSON column. In PostgreSQL this architecture target is JSONB: JSON stored in a binary form that can be indexed and queried efficiently while staying in the primary relational database.

`/api/v2` reads from MongoDB using the `analytics_events` collection. This compares a dedicated document store against the simpler Postgres JSONB baseline.

The capture path currently writes to Postgres first and attempts MongoDB as a secondary analytics sink, so normal product workflows still work when MongoDB is unavailable locally.

## Authorization Comparison

Authorization decisions are exposed through versioned comparison endpoints:

```text
POST /api/v1/workspaces/{workspaceId}/authorization/decisions
POST /api/v2/workspaces/{workspaceId}/authorization/decisions
```

`/api/v1` is RBAC: decisions are based on the actor's workspace role (`OWNER`, `ADMIN`, `MEMBER`).

`/api/v2` is ABAC/OPA-style: decisions also consider request/resource attributes such as `resourceOwnerUserId`, `assigneeUserId`, `resourceTenantId`, status, and `riskLevel`. It is implemented as a local OPA-compatible policy shape for comparison, not as an external OPA sidecar yet.

## Cache Strategy Comparison

Runtime cache access goes through one shared cache facade. Select the implementation with:

```text
PLATFORM_FEATURE_CACHE_MODE=caffeine
PLATFORM_FEATURE_CACHE_MODE=redis
PLATFORM_FEATURE_CACHE_MODE=memcached
```

Default local/test mode is `caffeine`, which needs no external infrastructure. `redis` uses the existing Spring `RedisTemplate` configuration. `memcached` uses a Memcached server configured by:

```text
PLATFORM_FEATURE_CACHE_MEMCACHED_HOST=localhost
PLATFORM_FEATURE_CACHE_MEMCACHED_PORT=11211
PLATFORM_FEATURE_CACHE_MEMCACHED_TIMEOUT_MS=1000
```

Direct comparison endpoints are available for authenticated benchmark calls:

```text
GET    /api/benchmarks/cache/strategy
POST   /api/benchmarks/cache/{strategy}/entries
GET    /api/benchmarks/cache/{strategy}/entries/{key}
POST   /api/benchmarks/cache/{strategy}/counters/{key}/increment
DELETE /api/benchmarks/cache/{strategy}/entries/{key}
```

Allowed `{strategy}` values are `caffeine`, `redis`, and `memcached`. Normal application code should keep using the shared cache facade so rate limiting and future cache-backed flows can be switched by configuration.

## Rate Limiting Algorithm Comparison

Rate limiting goes through one shared service. Select the default runtime algorithm with:

```text
PLATFORM_FEATURE_RATE_LIMIT_MODE=fixed-window
PLATFORM_FEATURE_RATE_LIMIT_MODE=sliding-window
PLATFORM_FEATURE_RATE_LIMIT_MODE=token-bucket
```

`fixed-window` is the local/test default and uses a bucketed counter in the configured cache backend. `sliding-window` stores recent request timestamps for stricter rolling-window behavior. `token-bucket` refills permits over time and allows short bursts up to the configured limit.

Direct comparison endpoints are available for authenticated benchmark calls:

```text
GET  /api/benchmarks/rate-limit/algorithm
POST /api/benchmarks/rate-limit/{algorithm}/decisions
```

Allowed `{algorithm}` values are `fixed-window`, `sliding-window`, and `token-bucket`. The request shape is the same for each algorithm:

```json
{
  "key": "user:123:global",
  "limit": 100,
  "windowSeconds": 60
}
```

## gRPC Internal API Experiment

The `experiment/grpc-internal-api` branch adds an optional gRPC server alongside the existing REST and GraphQL APIs. It is disabled by default so the REST-only backend remains the baseline.

Enable it with:

```text
PLATFORM_GRPC_ENABLED=true
PLATFORM_GRPC_PORT=9090
```

Current internal service:

```text
platform.v1.PlatformQueryService/ListTasks
platform.v1.PlatformQueryService/ListDocuments
```

The IDL lives at `src/main/proto/platform_query.proto`. The implementation currently uses standard protobuf `StringValue` requests and `Struct` responses so the branch can run without adding a generated-code build step yet.

## Observability

Complete observability infrastructure is included:

### Metrics & Monitoring
- **Prometheus-compatible metrics** export at `/actuator/prometheus`
- **Business metrics**: Task, document, event, and session tracking
- **System metrics**: JVM memory, GC, threads, CPU usage
- **Request metrics**: Response times, error rates, endpoint performance

### Structured Logging
- **Development**: Human-readable console logs with context
- **Production**: JSON logs with full MDC context
- **Async appenders**: Non-blocking performance
- **Separate error stream**: All errors captured and logged

### Distributed Tracing
- **Correlation IDs**: Track requests across the system
- **Trace IDs**: X-Trace-ID header for distributed tracing
- **MDC Context**: Automatic inclusion of context in all logs
- **Multi-tenant support**: Tenant ID tracking

### Health Checks
- REST endpoints for liveness and readiness probes
- Custom health indicators for platform services, events, and cache
- Kubernetes-compatible health check endpoints

### Documentation

Start here:
1. **[OBSERVABILITY_SUMMARY.md](./OBSERVABILITY_SUMMARY.md)** - Overview of what's implemented
2. **[OBSERVABILITY.md](./OBSERVABILITY.md)** - Complete feature documentation
3. **[OBSERVABILITY_INTEGRATION_GUIDE.md](./OBSERVABILITY_INTEGRATION_GUIDE.md)** - How to use in your code
4. **[OBSERVABILITY_ENDPOINTS.md](./OBSERVABILITY_ENDPOINTS.md)** - API reference and testing

### Quick Start

#### View Metrics
```bash
curl http://localhost:8080/actuator/metrics
curl http://localhost:8080/actuator/prometheus
```

#### Health Check
```bash
curl http://localhost:8080/actuator/health
```

#### View Available Endpoints
```bash
curl http://localhost:8080/actuator
```

#### In Your Code
```java
@Autowired
private BusinessMetricsCollector metrics;

// Record business metrics
metrics.recordTaskCreated();
metrics.recordTaskCompleted();

// Get correlation context
String correlationId = ObservabilityContext.getCorrelationId();
```
