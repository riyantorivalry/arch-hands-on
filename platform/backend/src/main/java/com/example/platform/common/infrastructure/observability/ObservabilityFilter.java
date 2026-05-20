package com.example.platform.common.infrastructure.observability;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Filter for HTTP request/response observability.
 * Adds correlation IDs, trace IDs, and captures request/response metadata.
 */
@Component
public class ObservabilityFilter implements WebFilter, Ordered {
    private static final Logger logger = LoggerFactory.getLogger(ObservabilityFilter.class);

    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String TRACE_ID_HEADER = "X-Trace-ID";
    private static final String REQUEST_ID_HEADER = "X-Request-ID";
    private static final String TENANT_ID_HEADER = "X-Tenant-ID";

    private static final String CORRELATION_ID_MDC = "correlationId";
    private static final String TRACE_ID_MDC = "traceId";
    private static final String REQUEST_ID_MDC = "requestId";
    private static final String TENANT_ID_MDC = "tenantId";
    private static final String METHOD_MDC = "method";
    private static final String PATH_MDC = "path";
    private static final String STATUS_MDC = "status";
    private static final String DURATION_MDC = "duration";

    @Autowired(required = false)
    private BusinessMetricsCollector metricsCollector;

    @Autowired(required = false)
    private Tracer tracer;

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (shouldNotFilter(exchange)) {
            return chain.filter(exchange);
        }

        long startTime = System.currentTimeMillis();
        String method = exchange.getRequest().getMethod().name();
        String path = exchange.getRequest().getPath().pathWithinApplication().value();
        String correlationId = getOrCreateCorrelationId(exchange);
        String traceId = getOrCreateTraceId(exchange);
        String requestId = UUID.randomUUID().toString();
        String tenantId = exchange.getRequest().getHeaders().getFirst(TENANT_ID_HEADER);

        // Set MDC values for logging
        MDC.put(CORRELATION_ID_MDC, correlationId);
        MDC.put(TRACE_ID_MDC, traceId);
        MDC.put(REQUEST_ID_MDC, requestId);
        if (tenantId != null) {
            MDC.put(TENANT_ID_MDC, tenantId);
        }
        MDC.put(METHOD_MDC, method);
        MDC.put(PATH_MDC, path);

        // Add response headers for client to use
        exchange.getResponse().getHeaders().set(CORRELATION_ID_HEADER, correlationId);
        exchange.getResponse().getHeaders().set(TRACE_ID_HEADER, traceId);
        exchange.getResponse().getHeaders().set(REQUEST_ID_HEADER, requestId);

        logger.debug("HTTP request started - method: {}, path: {}, correlationId: {}",
                method, path, correlationId);

        return chain.filter(exchange)
                .doFinally(signalType -> recordCompletion(exchange, method, path, startTime));
    }

    private void recordCompletion(ServerWebExchange exchange, String method, String path, long startTime) {
        long duration = System.currentTimeMillis() - startTime;
        HttpStatusCode status = exchange.getResponse().getStatusCode();
        int statusCode = status == null ? HttpStatus.OK.value() : status.value();
        MDC.put(STATUS_MDC, String.valueOf(statusCode));
        MDC.put(DURATION_MDC, String.valueOf(duration));

        logger.info("HTTP request completed - method: {}, path: {}, status: {}, duration: {}ms",
                method, path, statusCode, duration);

        // Record metrics if collector is available
        if (metricsCollector != null && statusCode >= 400) {
            metricsCollector.recordApiError();
        }

        MDC.remove(CORRELATION_ID_MDC);
        MDC.remove(TRACE_ID_MDC);
        MDC.remove(REQUEST_ID_MDC);
        MDC.remove(TENANT_ID_MDC);
        MDC.remove(METHOD_MDC);
        MDC.remove(PATH_MDC);
        MDC.remove(STATUS_MDC);
        MDC.remove(DURATION_MDC);
    }

    private String getOrCreateCorrelationId(ServerWebExchange exchange) {
        String correlationId = exchange.getRequest().getHeaders().getFirst(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isEmpty()) {
            correlationId = UUID.randomUUID().toString();
        }
        return correlationId;
    }

    private String getOrCreateTraceId(ServerWebExchange exchange) {
        if (tracer != null) {
            Span currentSpan = tracer.currentSpan();
            if (currentSpan != null && currentSpan.context() != null) {
                String traceId = currentSpan.context().traceId();
                if (traceId != null && !traceId.isBlank()) {
                    return traceId;
                }
            }
        }

        String traceId = exchange.getRequest().getHeaders().getFirst(TRACE_ID_HEADER);
        if (traceId == null || traceId.isEmpty()) {
            traceId = UUID.randomUUID().toString();
        }
        return traceId;
    }

    private boolean shouldNotFilter(ServerWebExchange exchange) {
        // Skip filtering for health check and metrics endpoints
        String path = exchange.getRequest().getPath().pathWithinApplication().value();
        return path.startsWith("/actuator/health") ||
               path.startsWith("/actuator/metrics") ||
               path.startsWith("/actuator/prometheus");
    }
}

