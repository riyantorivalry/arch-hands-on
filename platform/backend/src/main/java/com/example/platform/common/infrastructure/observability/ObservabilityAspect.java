package com.example.platform.common.infrastructure.observability;

import com.example.platform.common.web.AuthenticationRequiredException;
import com.example.platform.common.web.AuthorizationDeniedException;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

/**
 * Aspect for comprehensive observability of service methods.
 * Logs method entry/exit, exception handling, execution time, and emits trace spans.
 */
@Aspect
@Component
public class ObservabilityAspect {
    private static final Logger logger = LoggerFactory.getLogger(ObservabilityAspect.class);

    private final Tracer tracer;

    public ObservabilityAspect(Tracer tracer) {
        this.tracer = tracer;
    }

    /**
     * Track all service and application methods for observability.
     */
    @Around("execution(* com.example.platform..application..*(..)) || " +
            "execution(* com.example.platform..service..*(..)) || " +
            "execution(* com.example.platform..facade..*(..)) || " +
            "execution(* com.example.platform..api..*(..)) || " +
            "execution(* com.example.platform..processor..*(..)) || " +
            "execution(* com.example.platform..*Repository.*(..))")
    public Object trackMethodExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        String declaringTypeName = joinPoint.getSignature().getDeclaringTypeName();
        String shortClassName = joinPoint.getSignature().getDeclaringType().getSimpleName();
        String method = joinPoint.getSignature().getName();
        String methodName = declaringTypeName + "." + method;
        String layer = layer(declaringTypeName, shortClassName);
        String methodKey = "method";
        String executionTime = "executionTime";

        String previousMethod = MDC.get(methodKey);
        MDC.put(methodKey, methodName);

        Span span = tracer.nextSpan()
                .name(layer + "." + shortClassName + "." + method)
                .tag("code.namespace", declaringTypeName)
                .tag("code.function", method)
                .tag("platform.layer", layer);
        if (joinPoint.getSignature() instanceof MethodSignature methodSignature) {
            span.tag("code.signature", methodSignature.getMethod().toGenericString());
        }

        long startTime = System.currentTimeMillis();
        span.start();
        try (Tracer.SpanInScope ignored = tracer.withSpan(span)) {
            logger.debug("Method entry: {}", methodName);
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;
            span.tag("platform.duration_ms", String.valueOf(duration));
            MDC.put(executionTime, String.valueOf(duration));
            logger.debug("Method exit: {} - duration: {}ms", methodName, duration);
            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            span.tag("platform.duration_ms", String.valueOf(duration));
            span.error(e);
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

    private String layer(String declaringTypeName, String shortClassName) {
        if (declaringTypeName.contains(".api.")) {
            return "controller";
        }
        if (shortClassName.endsWith("Repository")) {
            return "repository";
        }
        if (declaringTypeName.contains(".application.") || shortClassName.endsWith("Facade")) {
            return "service";
        }
        if (declaringTypeName.contains(".processor.")) {
            return "processor";
        }
        if (declaringTypeName.contains(".service.")) {
            return "service";
        }
        return "method";
    }
}


