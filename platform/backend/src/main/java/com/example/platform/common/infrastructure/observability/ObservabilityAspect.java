package com.example.platform.common.infrastructure.observability;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

/**
 * Aspect for comprehensive observability of service methods.
 * Logs method entry/exit, exception handling, and execution time.
 */
@Aspect
@Component
public class ObservabilityAspect {
    private static final Logger logger = LoggerFactory.getLogger(ObservabilityAspect.class);

    /**
     * Track all service and application methods for observability.
     */
    @Around("execution(* com.example.platform..application..*(..)) || " +
            "execution(* com.example.platform..service..*(..)) || " +
            "execution(* com.example.platform..facade..*(..)) || " +
            "execution(* com.example.platform..api..*(..)) || " +
            "execution(* com.example.platform..processor..*(..))")
    public Object trackMethodExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getDeclaringTypeName() + "." +
                           joinPoint.getSignature().getName();
        String methodKey = "method";
        String executionTime = "executionTime";

        String previousMethod = MDC.get(methodKey);
        MDC.put(methodKey, methodName);

        long startTime = System.currentTimeMillis();
        try {
            logger.debug("Method entry: {}", methodName);
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;
            MDC.put(executionTime, String.valueOf(duration));
            logger.debug("Method exit: {} - duration: {}ms", methodName, duration);
            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            MDC.put(executionTime, String.valueOf(duration));
            logger.error("Method exception: {} - duration: {}ms - error: {}",
                    methodName, duration, e.getMessage(), e);
            throw e;
        } finally {
            if (previousMethod != null) {
                MDC.put(methodKey, previousMethod);
            } else {
                MDC.remove(methodKey);
            }
        }
    }
}


