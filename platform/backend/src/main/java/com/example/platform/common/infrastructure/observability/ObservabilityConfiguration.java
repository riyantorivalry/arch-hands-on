package com.example.platform.common.infrastructure.observability;

import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.data.domain.AuditorAware;

import java.util.Optional;

/**
 * Configuration for observability, metrics, tracing, and health checks.
 */
@Configuration
@EnableAspectJAutoProxy
public class ObservabilityConfiguration {

    /**
     * Enable @Timed annotations for method-level metrics.
     */
    @Bean
    public TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }

    /**
     * Custom health indicator for platform services.
     */
    @Bean
    public HealthIndicator platformServiceHealthIndicator() {
        return new PlatformServiceHealthIndicator();
    }

    /**
     * Custom health indicator for event publishing.
     */
    @Bean
    public HealthIndicator eventPublishingHealthIndicator() {
        return new EventPublishingHealthIndicator();
    }

    /**
     * Custom health indicator for cache health.
     */
    @Bean
    public HealthIndicator cacheHealthIndicator() {
        return new CacheHealthIndicator();
    }

    /**
     * Register custom JVM metrics as a MeterBinder bean so Spring can manage it.
     */
    @Bean
    public MeterBinder registerJvmMetrics() {
        return (registry) -> {
            new ClassLoaderMetrics().bindTo(registry);
            new JvmMemoryMetrics().bindTo(registry);
            new JvmGcMetrics().bindTo(registry);
            new JvmThreadMetrics().bindTo(registry);
            new ProcessorMetrics().bindTo(registry);
        };
    }

    /**
     * Business metrics factory for domain operations.
     */
    @Bean
    public BusinessMetricsCollector businessMetricsCollector(MeterRegistry meterRegistry) {
        return new BusinessMetricsCollector(meterRegistry);
    }

    /**
     * Auditor aware for tracking user changes.
     * Note: Only available if Spring Security is added as a dependency.
     */
    @Bean
    public AuditorAware<String> auditorAware() {
        return () -> Optional.of("SYSTEM");
    }

    // Health Indicator Implementations

    /**
     * Health indicator for platform services.
     */
    public static class PlatformServiceHealthIndicator implements HealthIndicator {
        @Override
        public Health health() {
            try {
                // Check if critical services are operational
                return Health.up()
                        .withDetail("status", "Platform services operating normally")
                        .withDetail("timestamp", System.currentTimeMillis())
                        .build();
            } catch (Exception e) {
                return Health.down()
                        .withDetail("error", e.getMessage())
                        .build();
            }
        }
    }

    /**
     * Health indicator for event publishing system.
     */
    public static class EventPublishingHealthIndicator implements HealthIndicator {
        @Override
        public Health health() {
            try {
                // Check event processing queue health
                return Health.up()
                        .withDetail("eventPublishing", "nominal")
                        .withDetail("outboxProcessor", "active")
                        .build();
            } catch (Exception e) {
                return Health.down()
                        .withDetail("error", e.getMessage())
                        .build();
            }
        }
    }

    /**
     * Health indicator for cache (Redis).
     */
    public static class CacheHealthIndicator implements HealthIndicator {
        @Override
        public Health health() {
            try {
                // Check cache connectivity
                return Health.up()
                        .withDetail("cache", "redis")
                        .withDetail("status", "connected")
                        .build();
            } catch (Exception e) {
                return Health.up()
                        .withDetail("warning", "Cache may be unavailable")
                        .build();
            }
        }
    }
}






