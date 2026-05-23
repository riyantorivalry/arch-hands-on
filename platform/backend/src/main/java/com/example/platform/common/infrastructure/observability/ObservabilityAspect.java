package com.example.platform.common.infrastructure.observability;

import com.example.platform.common.web.AuthenticationRequiredException;
import com.example.platform.common.web.AuthorizationDeniedException;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

/**
 * Aspect for comprehensive observability of service methods.
 * Logs method entry/exit, exception handling, execution time, and emits domain spans.
 *
 * Library/framework spans are intentionally left to the OpenTelemetry Java agent.
 * This aspect only covers application/domain methods that the agent cannot infer.
 */
@Aspect
@Component
public class ObservabilityAspect {
    private static final Logger logger = LoggerFactory.getLogger(ObservabilityAspect.class);
    private static final Tracer tracer = GlobalOpenTelemetry.getTracer("platform-backend-domain");

    /**
     * Track application/domain methods for observability.
     */
    @Around("execution(* com.example.platform..application..*(..)) || " +
            "execution(* com.example.platform..service..*(..)) || " +
            "execution(* com.example.platform..facade..*(..)) || " +
            "execution(* com.example.platform..processor..*(..))")
    public Object trackMethodExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        String declaringTypeName = joinPoint.getSignature().getDeclaringTypeName();
        String shortClassName = joinPoint.getSignature().getDeclaringType().getSimpleName();
        String method = joinPoint.getSignature().getName();
        String methodName = declaringTypeName + "." + method;
        String methodKey = "method";
        String executionTime = "executionTime";

        String previousMethod = MDC.get(methodKey);
        MDC.put(methodKey, methodName);

        Span span = tracer.spanBuilder("app." + shortClassName + "." + method)
                .setAttribute("code.namespace", declaringTypeName)
                .setAttribute("code.function", method)
                .setAttribute("platform.layer", "application")
                .startSpan();
        long startTime = System.currentTimeMillis();
        try (Scope ignored = span.makeCurrent()) {
            logger.debug("Method entry: {}", methodName);
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;
            span.setAttribute("platform.duration_ms", duration);
            MDC.put(executionTime, String.valueOf(duration));
            logger.debug("Method exit: {} - duration: {}ms", methodName, duration);
            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            span.setAttribute("platform.duration_ms", duration);
            span.recordException(e);
            span.setStatus(StatusCode.ERROR, e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
            MDC.put(executionTime, String.valueOf(duration));
            if (e instanceof AuthenticationRequiredException || e instanceof AuthorizationDeniedException) {
                logger.warn("Method denied: {} - duration: {}ms - error: {}",
                        methodName, duration, e.getMessage());
            } else {
                logger.error("Method exception: {} - duration: {}ms - error: {}",
                        methodName, duration, e.getMessage(), e);
            }
            throw e;
        } finally {
            span.end();
            if (previousMethod != null) {
                MDC.put(methodKey, previousMethod);
            } else {
                MDC.remove(methodKey);
            }
        }
    }
}


