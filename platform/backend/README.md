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

## Current scope

This scaffold establishes:

- one deployable Spring Boot app
- tenant-aware request context plumbing
- explicit module packages
- placeholder application services and controllers
- health and platform info endpoints

Feature persistence and domain rules should be added module by module from the Phase 1 contracts.

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
