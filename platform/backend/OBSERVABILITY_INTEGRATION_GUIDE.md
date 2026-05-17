# Backend Observability Integration Guide

## Quick Start

The observability infrastructure is automatically configured and active once the application starts. No additional setup is required for basic observability.

## Section 1: Using Metrics in Your Service

### 1.1 Recording Task Metrics

```java
import com.example.platform.common.infrastructure.observability.BusinessMetricsCollector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TaskService {
    
    @Autowired
    private BusinessMetricsCollector metrics;
    
    public Task createTask(TaskCreateCommand cmd) {
        metrics.recordTaskCreated();
        // ... create task
        return task;
    }
    
    public void completeTask(Long taskId) {
        var sample = metrics.startTaskProcessingTimer();
        try {
            // Process task
            metrics.recordTaskCompleted();
        } catch (Exception e) {
            metrics.recordTaskFailed();
            throw e;
        } finally {
            metrics.stopTaskProcessingTimer(sample);
        }
    }
}
```

### 1.2 Recording Document Metrics

```java
@Service
public class DocumentService {
    
    @Autowired
    private BusinessMetricsCollector metrics;
    
    public Document uploadDocument(FileUploadRequest request) {
        var sample = metrics.startDocumentProcessingTimer();
        try {
            metrics.recordDocumentUploaded();
            // ... upload logic
            return document;
        } finally {
            metrics.stopDocumentProcessingTimer(sample);
        }
    }
}
```

### 1.3 Recording Custom Metrics

```java
@Service
public class AnalyticsService {
    
    @Autowired
    private BusinessMetricsCollector metrics;
    
    public void recordAnalytics() {
        // Record gauge (current value)
        metrics.recordGaugeMetric("active.users", 150.0);
        
        // Record gauge with tags
        metrics.recordGaugeMetricWithTags(
            "queue.depth",
            queueSize,
            "queue.name", "event-processing",
            "priority", "high"
        );
        
        // Record event count
        metrics.recordCollaborationEvent();
    }
}
```

## Section 2: Using Correlation IDs and Context

### 2.1 Access Correlation Context

```java
import com.example.platform.common.infrastructure.observability.ObservabilityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class MyService {
    private static final Logger logger = LoggerFactory.getLogger(MyService.class);
    
    public void processRequest() {
        // Get context information
        String correlationId = ObservabilityContext.getCorrelationId();
        String traceId = ObservabilityContext.getTraceId();
        String tenantId = ObservabilityContext.getTenantId();
        
        logger.info("Processing for tenant: {}", tenantId);
        // Correlation ID is automatically in logs
    }
}
```

### 2.2 Set Context Manually (for async operations)

```java
@Service
public class AsyncTaskService {
    
    @Autowired
    private TaskRepository repository;
    
    @Async
    public void processTaskAsync(Long taskId) {
        // Copy context from request thread to async thread
        var context = ObservabilityContext.copyContext();
        
        try {
            // Your async logic
            var task = repository.findById(taskId);
        } finally {
            // Clear MDC after async operation
            ObservabilityContext.clearContext();
        }
    }
    
    public void submitTaskForAsync(Long taskId) {
        // Context will be available in the async method
        processTaskAsync(taskId);
    }
}
```

### 2.3 Builder Pattern for Context Setup

```java
public void setupCustomContext() {
    ObservabilityContext.builder()
        .correlationId("custom-123")
        .tenantId("acme-corp")
        .userId("user-456")
        .workspaceId("workspace-789")
        .apply();
    
    // Now all logs include this context
    logger.info("Request processed"); // Context included automatically
}
```

## Section 3: Using Aspect Annotations

### 3.1 Automatic Method Tracking

Service methods are automatically tracked:

```java
@Service
public class UserService {
    
    // This method is automatically tracked by ObservabilityAspect
    public User getUserById(Long id) {
        // Automatically logs:
        // - Method entry
        // - Execution time
        // - Method exit
        // - Any exceptions with stack trace
        return userRepository.findById(id);
    }
}
```

### 3.2 Adding @Timed Annotation for Custom Metrics

```java
import io.micrometer.core.annotation.Timed;

@Service
public class ReportGenerationService {
    
    @Timed(value = "report.generation", description = "Time to generate report")
    public ReportDTO generateReport(ReportRequest request) {
        // Execution time is recorded to "report.generation" metric
        return buildReport(request);
    }
    
    @Timed(value = "report.export", 
           description = "Time to export report",
           tags = {"format", "pdf"})
    public byte[] exportReportAsPdf(Long reportId) {
        // Tagged as format=pdf
        return exportToPdf(reportId);
    }
}
```

## Section 4: Health Indicators

### 4.1 Check Health Status

```bash
# Overall health
curl http://localhost:8080/actuator/health

# Detailed health
curl http://localhost:8080/actuator/health
# Response:
# {
#   "status": "UP",
#   "components": {
#     "platformService": {
#       "status": "UP",
#       "details": {}
#     },
#     "eventPublishing": {
#       "status": "UP",
#       "details": {}
#     },
#     "cache": {
#       "status": "UP",
#       "details": {}
#     }
#   }
# }

# Kubernetes probes
curl http://localhost:8080/actuator/health/live   # Liveness
curl http://localhost:8080/actuator/health/ready  # Readiness
```

### 4.2 Implementing Custom Health Indicator

```java
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class CustomServiceHealthIndicator implements HealthIndicator {
    
    @Override
    public Health health() {
        try {
            // Check your custom service
            boolean isHealthy = checkServiceHealth();
            
            if (isHealthy) {
                return Health.up()
                    .withDetail("service", "custom-service")
                    .withDetail("version", "1.0.0")
                    .build();
            } else {
                return Health.down()
                    .withDetail("error", "Service dependency unavailable")
                    .build();
            }
        } catch (Exception e) {
            return Health.down()
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}
```

## Section 5: Viewing Metrics

### 5.1 Available Metrics Endpoints

```bash
# List all available metrics
curl http://localhost:8080/actuator/metrics

# Get specific metric
curl http://localhost:8080/actuator/metrics/http.server.requests

# Get business metrics
curl http://localhost:8080/actuator/metrics/tasks.created
curl http://localhost:8080/actuator/metrics/documents.uploaded

# Prometheus format
curl http://localhost:8080/actuator/prometheus
```

### 5.2 Sample Prometheus Output

```
# HELP http_server_requests_seconds_max  
# TYPE http_server_requests_seconds_max gauge
http_server_requests_seconds_max{exception="none",method="GET",status="200",uri="/api/tasks"} 0.250

# HELP tasks_created_total  
# TYPE tasks_created_total counter
tasks_created_total{service="platform-backend",component="business"} 42.0

# HELP database_query_time_seconds  
# TYPE database_query_time_seconds summary
database_query_time_seconds_sum{service="platform-backend",component="business"} 12.345
database_query_time_seconds_count{service="platform-backend",component="business"} 100.0
```

## Section 6: Logging Examples

### Log Lines with Context

All logs automatically include correlation IDs:

```
2026-05-16 10:30:45.123 INFO [http-nio-8080-exec-1] [corr=550e8400-e29b-41d4 req=abc123 trace=xyz789 tenant=acme-corp user=john.doe] TaskService - Task created successfully
```

### JSON Log (Production)

```json
{
  "timestamp": "2026-05-16T10:30:45.123Z",
  "level": "INFO",
  "logger_name": "com.example.platform.tasks.application.TaskService",
  "thread_name": "http-nio-8080-exec-1",
  "message": "Task created successfully",
  "correlationId": "550e8400-e29b-41d4-a716-446655440000",
  "traceId": "550e8400-e29b-41d4-a716-446655440001",
  "tenantId": "acme-corp",
  "userId": "john.doe",
  "executionTime": "245"
}
```

## Section 7: Event Publishing Metrics

### 7.1 Track Event Publishing

```java
import com.example.platform.common.domain.DomainEventPublisher;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class TaskDomainService {
    
    @Autowired
    private DomainEventPublisher eventPublisher;
    
    @Autowired
    private BusinessMetricsCollector metrics;
    
    public void publishTaskEvent(TaskCreatedEvent event) {
        var sample = metrics.startEventPublishingTimer();
        try {
            eventPublisher.publish(event);
            metrics.recordEventPublished();
            logger.info("Event published: {}", event.getEventType());
        } catch (Exception e) {
            metrics.recordEventFailed();
            logger.error("Failed to publish event", e);
            throw e;
        } finally {
            metrics.stopEventPublishingTimer(sample);
        }
    }
}
```

## Section 8: Multi-Tenant Observability

### 8.1 Tracking Per-Tenant Metrics

```java
@RestController
@RequestMapping("/api/v1/tasks")
public class TaskController {
    
    @Autowired
    private TaskService taskService;
    
    @PostMapping
    public TaskDTO createTask(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @RequestBody TaskCreateRequest request) {
        
        // Tenant ID is automatically added to MDC by ObservabilityFilter
        // So it appears in all logs for this request
        
        return taskService.create(request);
    }
}
```

### 8.2 Querying by Tenant in Logs

```bash
# Filter JSON logs by tenant (using jq)
cat logs/platform-backend.json.log | jq 'select(.tenantId=="acme-corp")'

# With kail or similar tools
jq 'select(.tenantId=="acme-corp") | select(.level=="ERROR")' logs/platform-backend.json.log
```

## Section 9: Performance Monitoring

### 9.1 Check Slow Queries

```bash
# Get database query metrics
curl http://localhost:8080/actuator/metrics/database.query.time

# Output shows percentiles:
{
  "name": "database.query.time",
  "statistic": "MAX",
  "value": 1234.0
}
```

### 9.2 Monitor API Response Times

```bash
# Check response time histogram
curl http://localhost:8080/actuator/metrics/http.server.requests

# Get specific endpoint
curl "http://localhost:8080/actuator/metrics/http.server.requests?tag=uri:/api/tasks"
```

## Section 10: Troubleshooting

### Issue: Missing Correlation IDs

**Check:**
```bash
# Verify filter is active
curl -v http://localhost:8080/api/tasks
# Look for X-Correlation-ID in response headers
```

**Solution:**
- Ensure `ObservabilityFilter` is in the classpath
- Check if the filter is being excluded by `shouldNotFilter()`

### Issue: High Memory Usage

**Check:**
```bash
# Get gauge for memory usage
curl http://localhost:8080/actuator/metrics/jvm.memory.used

# Check if there's metric explosion
curl http://localhost:8080/actuator/metrics | grep -c "http.server.requests"
```

**Solution:**
- Limit URI tags to avoid metric explosion
- Use metric filtering in configuration

### Issue: No Custom Metrics Recording

**Check:**
```bash
# Verify BusinessMetricsCollector is autowired
curl http://localhost:8080/actuator/metrics | grep "tasks\|documents"
```

**Solution:**
- Ensure @Autowired dependency is properly injected
- Check if metrics recording method is being called
- Verify Micrometer beans are registered

## Section 11: Configuration Properties

### Application-specific Properties

Add to `application.yml`:

```yaml
# Logging
logging:
  level:
    root: INFO
    com.example.platform: DEBUG
    com.example.platform.common.infrastructure.observability: DEBUG
  file:
    name: logs/platform-backend.log
    max-size: 100MB

# Management/Observability
management:
  metrics:
    export:
      prometheus:
        enabled: true
  tracing:
    sampling:
      probability: 1.0  # 100% sampling (for dev/test)
```

## Section 12: Integration with Monitoring Stack

### Docker Compose with Prometheus + Grafana

```yaml
version: '3.8'
services:
  platform-backend:
    build: .
    ports:
      - "8080:8080"
    environment:
      SPRING_PROFILES_ACTIVE: prod

  prometheus:
    image: prom/prometheus:latest
    volumes:
      - ./prometheus.yml:/etc/prometheus/prometheus.yml
    ports:
      - "9090:9090"

  grafana:
    image: grafana/grafana:latest
    ports:
      - "3000:3000"
    depends_on:
      - prometheus
```

### Prometheus Configuration

```yaml
# prometheus.yml
global:
  scrape_interval: 15s

scrape_configs:
  - job_name: 'platform-backend'
    static_configs:
      - targets: ['localhost:8080']
    metrics_path: '/actuator/prometheus'
```

## Next Steps

1. **Test locally:** Run backend and visit `http://localhost:8080/actuator/metrics`
2. **Add to controllers:** Use `BusinessMetricsCollector` in your endpoints
3. **Set up Prometheus:** Configure scraping from your backend
4. **Create Grafana dashboards:** Visualize metrics in real-time
5. **Configure alerting:** Set up alerts for critical metrics
6. **Review logs:** Check JSON logs for troubleshooting

