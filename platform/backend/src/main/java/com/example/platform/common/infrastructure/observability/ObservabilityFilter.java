package com.example.platform.common.infrastructure.observability;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

/**
 * Filter for HTTP request/response observability.
 * Adds correlation IDs, trace IDs, and captures request/response metadata.
 */
@Component
public class ObservabilityFilter extends OncePerRequestFilter {
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

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        long startTime = System.currentTimeMillis();
        String correlationId = getOrCreateCorrelationId(request);
        String traceId = getOrCreateTraceId(request);
        String requestId = UUID.randomUUID().toString();
        String tenantId = request.getHeader(TENANT_ID_HEADER);

        // Set MDC values for logging
        MDC.put(CORRELATION_ID_MDC, correlationId);
        MDC.put(TRACE_ID_MDC, traceId);
        MDC.put(REQUEST_ID_MDC, requestId);
        if (tenantId != null) {
            MDC.put(TENANT_ID_MDC, tenantId);
        }
        MDC.put(METHOD_MDC, request.getMethod());
        MDC.put(PATH_MDC, request.getRequestURI());

        // Add response headers for client to use
        response.setHeader(CORRELATION_ID_HEADER, correlationId);
        response.setHeader(TRACE_ID_HEADER, traceId);
        response.setHeader(REQUEST_ID_HEADER, requestId);

        try {
            logger.info("HTTP request started - method: {}, path: {}, correlationId: {}",
                    request.getMethod(), request.getRequestURI(), correlationId);

            filterChain.doFilter(request, response);

        } finally {
            long duration = System.currentTimeMillis() - startTime;
            MDC.put(STATUS_MDC, String.valueOf(response.getStatus()));
            MDC.put(DURATION_MDC, String.valueOf(duration));

            logger.info("HTTP request completed - method: {}, path: {}, status: {}, duration: {}ms",
                    request.getMethod(), request.getRequestURI(), response.getStatus(), duration);

            // Record metrics if collector is available
            if (metricsCollector != null) {
                if (response.getStatus() >= 400) {
                    metricsCollector.recordApiError();
                }
            }

            // Clear MDC
            MDC.clear();
        }
    }

    private String getOrCreateCorrelationId(HttpServletRequest request) {
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isEmpty()) {
            correlationId = UUID.randomUUID().toString();
        }
        return correlationId;
    }

    private String getOrCreateTraceId(HttpServletRequest request) {
        String traceId = request.getHeader(TRACE_ID_HEADER);
        if (traceId == null || traceId.isEmpty()) {
            traceId = UUID.randomUUID().toString();
        }
        return traceId;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        // Skip filtering for health check and metrics endpoints
        String path = request.getRequestURI();
        return path.startsWith("/actuator/health") ||
               path.startsWith("/actuator/metrics") ||
               path.startsWith("/actuator/prometheus");
    }
}

