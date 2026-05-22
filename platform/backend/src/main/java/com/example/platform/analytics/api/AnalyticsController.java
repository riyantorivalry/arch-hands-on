package com.example.platform.analytics.api;

import com.example.platform.analytics.application.AnalyticsService;
import com.example.platform.analytics.application.AnalyticsService.AnalyticsEventView;
import com.example.platform.common.web.AuthorizationDeniedException;
import com.example.platform.common.web.RequestContexts;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/v1/tenants/{tenantId}/analytics/events")
    public Mono<List<AnalyticsEventView>> listPostgresJsonbEvents(
            @PathVariable String tenantId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant end
    ) {
        return requireSameTenant(tenantId)
                .then(Mono.defer(() -> {
                    TimeRange range = normalizeRange(start, end);
                    return analyticsService.getPostgresJsonbEventsForTenant(tenantId, range.start(), range.end());
                }));
    }

    @GetMapping("/v2/tenants/{tenantId}/analytics/events")
    public Mono<List<AnalyticsEventView>> listMongoEvents(
            @PathVariable String tenantId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant end
    ) {
        return requireSameTenant(tenantId)
                .then(Mono.defer(() -> {
                    TimeRange range = normalizeRange(start, end);
                    return analyticsService.getMongoEventsForTenant(tenantId, range.start(), range.end());
                }));
    }

    private Mono<Void> requireSameTenant(String tenantId) {
        return RequestContexts.authenticatedReactive()
                .flatMap(context -> {
                    if (!tenantId.equals(context.tenantId())) {
                        return Mono.error(new AuthorizationDeniedException("Actor is not allowed to read analytics for tenant " + tenantId));
                    }
                    return Mono.empty();
                });
    }

    private TimeRange normalizeRange(Instant start, Instant end) {
        Instant normalizedEnd = end == null ? Instant.now() : end;
        Instant normalizedStart = start == null ? normalizedEnd.minus(30, ChronoUnit.DAYS) : start;
        if (normalizedStart.isAfter(normalizedEnd)) {
            throw new IllegalArgumentException("start must be before end");
        }
        return new TimeRange(normalizedStart, normalizedEnd);
    }

    private record TimeRange(Instant start, Instant end) {
    }
}
