/**
 * Observability infrastructure module providing:
 * - Distributed tracing with correlation IDs
 * - Structured logging with MDC (Mapped Diagnostic Context)
 * - Business and system metrics collection
 * - Health indicators and monitoring endpoints
 * - Request/response tracking with filters
 * - Method-level observability through AOP
 *
 * This module integrates:
 * - Spring Boot Actuator for metrics and health checks
 * - Micrometer for metrics collection
 * - Micrometer Tracing with OpenTelemetry export
 * - Logback with JSON encoding for structured logs
 * - AspectJ for method-level instrumentation
 *
 * @author Platform Team
 * @version 1.0
 */
package com.example.platform.common.infrastructure.observability;

