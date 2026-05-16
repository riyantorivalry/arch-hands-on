package com.example.platform.common.infrastructure.observability;

import org.slf4j.MDC;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for observability operations.
 * Provides helpers for correlation ID management, MDC operations, and context tracking.
 */
public class ObservabilityContext {

    private static final String CORRELATION_ID = "correlationId";
    private static final String TRACE_ID = "traceId";
    private static final String REQUEST_ID = "requestId";
    private static final String TENANT_ID = "tenantId";
    private static final String USER_ID = "userId";
    private static final String WORKSPACE_ID = "workspaceId";

    /**
     * Set correlation ID for tracing request flow.
     */
    public static void setCorrelationId(String correlationId) {
        MDC.put(CORRELATION_ID, correlationId);
    }

    /**
     * Get correlation ID.
     */
    public static String getCorrelationId() {
        return MDC.get(CORRELATION_ID);
    }

    /**
     * Set trace ID for distributed tracing.
     */
    public static void setTraceId(String traceId) {
        MDC.put(TRACE_ID, traceId);
    }

    /**
     * Get trace ID.
     */
    public static String getTraceId() {
        return MDC.get(TRACE_ID);
    }

    /**
     * Set request ID.
     */
    public static void setRequestId(String requestId) {
        MDC.put(REQUEST_ID, requestId);
    }

    /**
     * Get request ID.
     */
    public static String getRequestId() {
        return MDC.get(REQUEST_ID);
    }

    /**
     * Set tenant ID for multi-tenancy context.
     */
    public static void setTenantId(String tenantId) {
        if (tenantId != null) {
            MDC.put(TENANT_ID, tenantId);
        }
    }

    /**
     * Get tenant ID.
     */
    public static String getTenantId() {
        return MDC.get(TENANT_ID);
    }

    /**
     * Set user ID.
     */
    public static void setUserId(String userId) {
        if (userId != null) {
            MDC.put(USER_ID, userId);
        }
    }

    /**
     * Get user ID.
     */
    public static String getUserId() {
        return MDC.get(USER_ID);
    }

    /**
     * Set workspace ID.
     */
    public static void setWorkspaceId(String workspaceId) {
        if (workspaceId != null) {
            MDC.put(WORKSPACE_ID, workspaceId);
        }
    }

    /**
     * Get workspace ID.
     */
    public static String getWorkspaceId() {
        return MDC.get(WORKSPACE_ID);
    }

    /**
     * Get all MDC context as a map.
     */
    public static Map<String, String> getContext() {
        return new HashMap<>(MDC.getCopyOfContextMap() != null ? MDC.getCopyOfContextMap() : new HashMap<>());
    }

    /**
     * Clear all MDC context.
     */
    public static void clearContext() {
        MDC.clear();
    }

    /**
     * Copy context from another map (useful for async operations).
     */
    public static Map<String, String> copyContext() {
        Map<String, String> context = MDC.getCopyOfContextMap();
        return context != null ? new HashMap<>(context) : new HashMap<>();
    }

    /**
     * Set context from a map (useful for async operations).
     */
    public static void setContext(Map<String, String> context) {
        if (context != null) {
            context.forEach(MDC::put);
        }
    }

    /**
     * Builder for fluent context setup.
     */
    public static class ContextBuilder {
        private String correlationId;
        private String traceId;
        private String requestId;
        private String tenantId;
        private String userId;
        private String workspaceId;

        public ContextBuilder correlationId(String id) {
            this.correlationId = id;
            return this;
        }

        public ContextBuilder traceId(String id) {
            this.traceId = id;
            return this;
        }

        public ContextBuilder requestId(String id) {
            this.requestId = id;
            return this;
        }

        public ContextBuilder tenantId(String id) {
            this.tenantId = id;
            return this;
        }

        public ContextBuilder userId(String id) {
            this.userId = id;
            return this;
        }

        public ContextBuilder workspaceId(String id) {
            this.workspaceId = id;
            return this;
        }

        public void apply() {
            if (correlationId != null) setCorrelationId(correlationId);
            if (traceId != null) setTraceId(traceId);
            if (requestId != null) setRequestId(requestId);
            if (tenantId != null) setTenantId(tenantId);
            if (userId != null) setUserId(userId);
            if (workspaceId != null) setWorkspaceId(workspaceId);
        }
    }

    /**
     * Create a context builder.
     */
    public static ContextBuilder builder() {
        return new ContextBuilder();
    }
}

