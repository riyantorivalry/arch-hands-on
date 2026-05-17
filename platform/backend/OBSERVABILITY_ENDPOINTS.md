# Observability Endpoints Reference

## Quick Test Commands

### Start the Backend
```bash
cd platform/backend
mvn spring-boot:run
```

The backend will be available at `http://localhost:8080`

## Health Checks

### 1. Overall Health Status
```bash
curl http://localhost:8080/actuator/health
```

**Response:**
```json
{
  "status": "UP",
  "components": {
    "cacheHealthIndicator": {
      "status": "UP",
      "details": {
        "cache": "redis",
        "status": "connected"
      }
    },
    "db": {
      "status": "UP",
      "details": {
        "database": "PostgreSQL"
      }
    },
    "diskSpace": {
      "status": "UP",
      "details": {}
    },
    "eventPublishingHealthIndicator": {
      "status": "UP",
      "details": {
        "eventPublishing": "nominal",
        "outboxProcessor": "active"
      }
    },
    "livenessState": {
      "status": "UP"
    },
    "platformServiceHealthIndicator": {
      "status": "UP",
      "details": {
        "status": "Platform services operating normally"
      }
    },
    "readinessState": {
      "status": "UP"
    }
  }
}
```

### 2. Liveness Probe (Kubernetes)
```bash
curl http://localhost:8080/actuator/health/live
```

**Response:**
```json
{
  "status": "UP"
}
```

### 3. Readiness Probe (Kubernetes)
```bash
curl http://localhost:8080/actuator/health/ready
```

**Response:**
```json
{
  "status": "UP",
  "components": {
    "db": { "status": "UP" },
    "diskSpace": { "status": "UP" }
  }
}
```

## Metrics

### 1. List All Available Metrics
```bash
curl http://localhost:8080/actuator/metrics
```

**Response:**
```json
{
  "names": [
    "api.errors",
    "api.response.time",
    "collaboration.events",
    "collaboration.latency",
    "database.query.time",
    "documents.accessed",
    "documents.uploaded",
    "event.publishing.time",
    "events.failed",
    "events.published",
    "http.server.requests",
    "jvm.memory.used",
    "jvm.gc.memory.allocated",
    "jvm.threads.peak",
    "process.cpu.usage",
    "sessions.created",
    "task.processing.time",
    "tasks.completed",
    "tasks.created",
    "tasks.failed"
  ]
}
```

### 2. Get Specific Metric Details

#### Task Creation Counter
```bash
curl http://localhost:8080/actuator/metrics/tasks.created
```

**Response:**
```json
{
  "name": "tasks.created",
  "description": "Total tasks created",
  "baseUnit": null,
  "measurements": [
    {
      "statistic": "COUNT",
      "value": 42.0
    }
  ],
  "availableTags": [
    {
      "tag": "service",
      "values": ["platform-backend"]
    },
    {
      "tag": "component",
      "values": ["business"]
    }
  ]
}
```

#### Task Processing Time
```bash
curl http://localhost:8080/actuator/metrics/task.processing.time
```

**Response:**
```json
{
  "name": "task.processing.time",
  "description": "Time taken to process a task",
  "baseUnit": "seconds",
  "measurements": [
    {
      "statistic": "COUNT",
      "value": 10.0
    },
    {
      "statistic": "TOTAL_TIME",
      "value": 5.234
    },
    {
      "statistic": "MAX",
      "value": 0.845
    }
  ],
  "availableTags": []
}
```

#### HTTP Request Metrics
```bash
curl http://localhost:8080/actuator/metrics/http.server.requests
```

**Response:**
```json
{
  "name": "http.server.requests",
  "description": "HTTP requests",
  "baseUnit": "seconds",
  "measurements": [
    {
      "statistic": "COUNT",
      "value": 156.0
    },
    {
      "statistic": "TOTAL_TIME",
      "value": 12.5
    },
    {
      "statistic": "MAX",
      "value": 1.2
    }
  ],
  "availableTags": [
    {
      "tag": "method",
      "values": ["GET", "POST", "PUT", "DELETE"]
    },
    {
      "tag": "uri",
      "values": ["/api/tasks", "/api/documents", "/actuator/health"]
    },
    {
      "tag": "status",
      "values": ["200", "201", "400", "404"]
    }
  ]
}
```

### 3. Filter Metrics by Tag
```bash
# Get metrics only for POST requests
curl "http://localhost:8080/actuator/metrics/http.server.requests?tag=method:POST"

# Get metrics only for /api/tasks endpoint
curl "http://localhost:8080/actuator/metrics/http.server.requests?tag=uri:/api/tasks"

# Get metrics for successful responses only
curl "http://localhost:8080/actuator/metrics/http.server.requests?tag=status:200"
```

### 4. JVM Memory Metrics
```bash
curl http://localhost:8080/actuator/metrics/jvm.memory.used
```

**Response:**
```json
{
  "name": "jvm.memory.used",
  "description": "The amount of used memory",
  "baseUnit": "bytes",
  "measurements": [
    {
      "statistic": "VALUE",
      "value": 342157312.0
    }
  ],
  "availableTags": [
    {
      "tag": "area",
      "values": ["heap", "nonheap"]
    },
    {
      "tag": "id",
      "values": ["PS Survivor Space", "PS Old Gen", "Metaspace"]
    }
  ]
}
```

### 5. Prometheus Export
```bash
curl http://localhost:8080/actuator/prometheus
```

**Response (text format):**
```
# HELP tasks_created_total Total tasks created
# TYPE tasks_created_total counter
tasks_created_total{component="business",service="platform-backend"} 42.0

# HELP http_server_requests_seconds HTTP requests
# TYPE http_server_requests_seconds summary
http_server_requests_seconds{method="POST",status="201",uri="/api/tasks"} 0.234
```

## Application Info

### Get Application Information
```bash
curl http://localhost:8080/actuator/info
```

**Response:**
```json
{
  "app": {
    "name": "platform-backend",
    "description": "Principal engineer hands-on collaboration platform backend",
    "version": "0.0.1-SNAPSHOT"
  }
}
```

## Logger Management

### List All Loggers
```bash
curl http://localhost:8080/actuator/loggers
```

### Get Specific Logger Level
```bash
curl http://localhost:8080/actuator/loggers/com.example.platform
```

**Response:**
```json
{
  "configuredLevel": null,
  "effectiveLevel": "DEBUG"
}
```

### Change Logger Level
```bash
# Enable DEBUG for a specific logger
curl -X POST http://localhost:8080/actuator/loggers/com.example.platform \
  -H "Content-Type: application/json" \
  -d '{"configuredLevel": "DEBUG"}'

# Set to ERROR
curl -X POST http://localhost:8080/actuator/loggers/com.example.platform \
  -H "Content-Type: application/json" \
  -d '{"configuredLevel": "ERROR"}'
```

## Diagnostics

### Thread Dump
```bash
curl http://localhost:8080/actuator/threaddump > thread-dump.json
```

### Heap Dump
```bash
curl http://localhost:8080/actuator/heapdump > heap-dump.hprof
```

Use tools like JProfiler or MAT to analyze the heap dump.

## Testing with Correlation IDs

### Make a Request with Correlation ID
```bash
curl -i http://localhost:8080/api/tasks \
  -H "X-Correlation-ID: my-custom-id-123" \
  -H "X-Tenant-ID: acme-corp"
```

**Check Response Headers:**
```
X-Correlation-ID: my-custom-id-123
X-Trace-ID: 550e8400-e29b-41d4-a716-446655440000
X-Request-ID: abc-def-ghi-jkl
```

### View Logs with Correlation ID
Check your logs (development console or JSON file):
```bash
# For JSON logs
tail -f logs/platform-backend.json.log | jq 'select(.correlationId=="my-custom-id-123")'

# For error logs
grep "my-custom-id-123" logs/platform-backend.error.log
```

## Scripting/Automation

### Monitor Metrics in Real-Time
```bash
#!/bin/bash
while true; do
  clear
  echo "=== Platform Backend Metrics ==="
  curl -s http://localhost:8080/actuator/metrics/tasks.created | jq '.measurements'
  curl -s http://localhost:8080/actuator/metrics/documents.uploaded | jq '.measurements'
  curl -s http://localhost:8080/actuator/metrics/events.published | jq '.measurements'
  echo "Last updated: $(date)"
  sleep 5
done
```

### Health Check Script
```bash
#!/bin/bash
HEALTH=$(curl -s http://localhost:8080/actuator/health | jq '.status' -r)
if [ "$HEALTH" = "UP" ]; then
  echo "✓ Backend is healthy"
  exit 0
else
  echo "✗ Backend is unhealthy"
  exit 1
fi
```

### Collect Metrics for Alerting
```bash
#!/bin/bash
ERROR_RATE=$(curl -s "http://localhost:8080/actuator/metrics/api.errors" | jq '.measurements[0].value' -r)
if (( $(echo "$ERROR_RATE > 10" | bc -l) )); then
  echo "ALERT: High error rate detected: $ERROR_RATE"
  # Send alert to monitoring system
fi
```

## Docker Testing

### Run with Docker Compose
```yaml
# docker-compose.yml
version: '3.8'
services:
  backend:
    build: ./platform/backend
    ports:
      - "8080:8080"
    environment:
      SPRING_PROFILES_ACTIVE: prod
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 10s
      timeout: 5s
      retries: 3
```

Run:
```bash
docker-compose up
```

## Monitoring Dashboard URLs (Local)

Once you set up the full stack, access:

- **Backend:** http://localhost:8080
- **Prometheus:** http://localhost:9090
- **Grafana:** http://localhost:3000
- **Application Logs:** `logs/platform-backend.json.log`

## Common Issues & Solutions

### Metrics not appearing
**Solution:** Ensure you've made requests to the backend. Metrics are recorded only when operations occur.

### Health check returns DOWN
```bash
curl http://localhost:8080/actuator/health
# Check individual components
curl http://localhost:8080/actuator/health/ping
```

### Logs not in JSON format
**Check:** Ensure you're running with `prod` profile or checking the JSON file (`logs/platform-backend.json.log`)

### Cannot access actuator endpoints
**Check firewall:** `netstat -an | grep 8080`
**Check logs:** `tail -f logs/platform-backend.error.log`

---

**Last Updated:** May 16, 2026
**Version:** 1.0

