# Platform Backend Observability Guide

## Overview

This backend implements comprehensive observability covering three pillars: **Metrics**, **Logging**, and **Tracing**. This enables production-grade monitoring, debugging, and performance analysis.

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    HTTP Requests                             │
└────────────────────── │ ──────────────────────────────────────┘
                        │
                        ▼
            ┌───────────────────────────┐
            │  ObservabilityFilter      │
            │  (Correlation IDs, MDC)   │
            └───────────────────────────┘
                        │
              ┌─────────┼─────────┐
              ▼         ▼         ▼
         ┌────────┐ ┌──────┐ ┌────────┐
         │ Service│ │Aspect│ │Metrics │
         │ Code   │ │Logs  │ │Collector
         └────────┘ └──────┘ └────────┘
              │         │         │
              └─────────┼─────────┘
                        │
              ┌─────────┼─────────┐
              ▼         ▼         ▼
        ┌─────────┐ ┌───────┐ ┌────────┐
        │  Logs   │ │Traces │ │Metrics │
        │ Storage │ │Export │ │Prometheus
        └─────────┘ └───────┘ └────────┘
```

## Components

### 1. ObservabilityConfiguration
- Registers custom metrics collectors
- Configures health indicators
- Enables AspectJ auto-proxy for method tracking
- Registers JVM metrics (memory, GC, threads)

**Key Health Indicators:**
- `platformService` - Platform services health
- `eventPublishing` - Event processing system health
- `cache` - Redis cache health

### 2. BusinessMetricsCollector
Collects domain-specific metrics:
- **Task Metrics**: Created, completed, failed, processing time
- **Document Metrics**: Uploaded, accessed, processing time
- **Session Metrics**: Created sessions
- **Collaboration Metrics**: Events, latency
- **Event Publishing**: Published, failed, publish time
- **API Metrics**: Errors, response time
- **Database Metrics**: Query time

### 3. ObservabilityFilter
HTTP request/response tracking:
- Extracts or generates correlation IDs
- Generates trace IDs and request IDs
- Captures tenant information
- Sets MDC (Mapped Diagnostic Context)
- Adds response headers for tracing
- Tracks HTTP duration and status

**MDC Fields:**
```
correlationId   - Unique ID for request flow tracking
traceId        - Distributed trace identifier
requestId      - Unique request identifier
tenantId       - Multi-tenant identifier
method         - HTTP method
path           - Request path
status         - HTTP response status
duration       - Request duration in ms
```

### 4. ObservabilityAspect
Method-level observability:
- Tracks method entry/exit
- Captures execution time
- Logs exceptions with context
- Updates MDC during execution
- Pointcuts: service, application, facade, API, processor methods

### 5. Structured Logging (logback-spring.xml)
Two logging modes:
- **Development**: Console output + error file
- **Production**: JSON structured logs + error file

**Features:**
- Async appenders for performance
- Rolling file policies (daily + size-based)
- JSON encoding for easy parsing
- MDC context included in logs
- Separate error log stream

## Endpoints

### Health Checks
- `/actuator/health` - Overall health status
- `/actuator/health/live` - Kubernetes liveness probe
- `/actuator/health/ready` - Kubernetes readiness probe

### Metrics
- `/actuator/metrics` - Metric names list
- `/actuator/metrics/{metric}` - Individual metric details
- `/actuator/prometheus` - Prometheus-compatible output

### Management
- `/actuator/info` - Application information
- `/actuator/loggers` - Logger management
- `/actuator/threaddump` - Thread dump
- `/actuator/heapdump` - Heap dump

## Key Metrics

### Request Metrics
```
http.server.requests
  - Count
  - Total time
  - Max time
  - Percentiles (50%, 95%, 99%)
  - Tags: method, uri, status
```

### Business Metrics
```
tasks.created              - Counter
tasks.completed            - Counter
tasks.failed               - Counter
task.processing.time       - Timer
documents.uploaded         - Counter
documents.accessed         - Counter
document.processing.time   - Timer
sessions.created           - Counter
collaboration.events       - Counter
collaboration.latency      - Timer
events.published           - Counter
events.failed              - Counter
event.publishing.time      - Timer
api.errors                 - Counter
api.response.time          - Timer
database.query.time        - Timer
```

### System Metrics
```
jvm.memory.used            - JVM heap/non-heap usage
jvm.gc.memory.allocated    - GC allocation rate
jvm.threads.peak           - Peak thread count
process.cpu.usage          - CPU usage
process.files.open         - Open file descriptors
```

## Configuration

### Application.yml
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus,loggers,threaddump,heapdump
  metrics:
    export:
      prometheus:
        enabled: true
  tracing:
    sampling:
      probability: 1.0
```

### Logging levels
```yaml
logging:
  level:
    root: INFO
    com.example.platform: DEBUG
    org.springframework: INFO
```

## Usage Examples

### 1. Tracking Task Processing
```java
@Service
public class TaskService {
    @Autowired
    private BusinessMetricsCollector metrics;

    public Task createTask(TaskCommand cmd) {
        metrics.recordTaskCreated();
        
        Timer.Sample sample = metrics.startTaskProcessingTimer();
        try {
            // Process task
            metrics.recordTaskCompleted();
            return result;
        } catch (Exception e) {
            metrics.recordTaskFailed();
            throw e;
        } finally {
            metrics.stopTaskProcessingTimer(sample);
        }
    }
}
```

### 2. Custom Gauge Metrics
```java
metrics.recordGaugeMetric("active.sessions", activeCount);
metrics.recordGaugeMetricWithTags(
    "queue.depth", 
    queueSize,
    "queue", "event-processing"
);
```

### 3. Accessing Correlation ID
```java
String correlationId = MDC.get("correlationId");
String traceId = MDC.get("traceId");
// Available throughout the request chain
```

## Monitoring & Alerting

### Prometheus Integration
Scrape endpoint: `http://localhost:8080/actuator/prometheus`

**Example Prometheus scrape config:**
```yaml
scrape_configs:
  - job_name: 'platform-backend'
    static_configs:
      - targets: ['localhost:8080']
    metrics_path: '/actuator/prometheus'
```

### Alert Rules (Example)
```yaml
groups:
  - name: platform-backend
    rules:
      - alert: HighErrorRate
        expr: rate(api.errors[5m]) > 0.05
        annotations:
          summary: "High API error rate"

      - alert: SlowDB
        expr: database.query.time{quantile="0.95"} > 1000
        annotations:
          summary: "Database queries are slow"

      - alert: EventPublishingFailures
        expr: rate(events.failed[5m]) > 0.01
        annotations:
          summary: "Event publishing failures detected"
```

## Log Aggregation

### JSON Logs Structure
```json
{
  "timestamp": "2026-05-16T10:30:45.123Z",
  "level": "INFO",
  "logger_name": "com.example.platform.tasks.application.TaskService",
  "thread_name": "http-nio-8080-exec-1",
  "message": "Task completed successfully",
  "correlationId": "550e8400-e29b-41d4-a716-446655440000",
  "traceId": "550e8400-e29b-41d4-a716-446655440001",
  "tenantId": "tenant-123",
  "method": "createTask",
  "executionTime": "245"
}
```

### Live Log Streaming
```bash
# Tail JSON logs with jq
tail -f logs/platform-backend.json.log | jq 'select(.level=="ERROR")'

# Filter by correlation ID
tail -f logs/platform-backend.json.log | jq 'select(.correlationId=="550e8400-e29b-41d4-a716-446655440000")'

# Filter by tenant
tail -f logs/platform-backend.json.log | jq 'select(.tenantId=="tenant-123")'
```

## Best Practices

1. **Always use correlation IDs**: Enable request tracing across services
2. **Structured logging**: Use JSON logs in production for easy parsing
3. **Business metrics**: Instrument critical business operations
4. **Tag everything**: Use tags to filter and aggregate metrics
5. **Sampling in production**: Reduce tracing overhead with sampling probability
6. **Log levels**: DEBUG in dev, INFO in production
7. **Health checks**: Use liveness and readiness probes for orchestration
8. **Async appenders**: Use async loggers to avoid blocking I/O
9. **Retention policies**: Implement log rotation and retention
10. **Security**: Don't log sensitive data (passwords, tokens, PII)

## Performance Considerations

- **Async logging**: Reduces latency by ~95% vs synchronous logging
- **Sampling**: Set `tracing.sampling.probability` < 1.0 for high-throughput systems
- **Metrics**: Micrometer is optimized for zero-allocation metrics collection
- **MDC overhead**: Minimal (<1% overhead) for correlation tracking

## Troubleshooting

### Issue: Missing correlation IDs in logs
**Solution**: Ensure ObservabilityFilter is active and requests include X-Correlation-ID header

### Issue: High memory usage by metrics
**Solution**: Check for metric explosion (too many tag combinations). Use meter filtering:
```java
meterRegistry.config()
    .meterFilter(MeterFilter.deny(meter -> 
        meter.getId().getName().contains("uri")))
    .meterFilter(MeterFilter.maximumAllowableTags("uri", 100, MeterFilter.deny()));
```

### Issue: Missing JVM metrics
**Solution**: Verify ObservabilityConfiguration is loaded and JVM metrics binding is registered

## Integration Examples

### With ELK Stack
```yaml
# Logstash pipeline
input {
  file {
    path => "/path/to/logs/platform-backend.json.log"
    codec => json
  }
}

output {
  elasticsearch {
    hosts => ["localhost:9200"]
    index => "platform-backend-%{+YYYY.MM.dd}"
  }
}
```

### With Grafana Loki
```yaml
scrape_configs:
  - job_name: platform-backend-logs
    static_configs:
      - targets:
          - localhost
        labels:
          job: platform-backend
          __path__: /path/to/logs/*.json.log
    pipeline_stages:
      - json:
          expressions:
            level: level
            logger: logger_name
            correlation_id: correlationId
```

### With Grafana Dashboards
Import or create dashboards to visualize:
- Request rate and latency
- Error rates by endpoint
- Task completion metrics
- Event processing throughput
- System resource usage (CPU, memory, GC)
- Database query performance

## Next Steps

1. Deploy with monitoring stack (Prometheus + Grafana + Loki)
2. Define SLOs and alerting rules
3. Integrate with incident management (PagerDuty, OpsGenie)
4. Set up log retention policies
5. Create custom dashboards for business metrics
6. Implement trace sampling strategy
7. Add distributed tracing backend (Jaeger/Zipkin) if needed

