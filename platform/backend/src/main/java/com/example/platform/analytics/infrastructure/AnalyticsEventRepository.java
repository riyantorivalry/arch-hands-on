package com.example.platform.analytics.infrastructure;

import com.example.platform.analytics.domain.AnalyticsEventEntity;
import java.time.Instant;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface AnalyticsEventRepository extends ReactiveCrudRepository<AnalyticsEventEntity, String> {

    Flux<AnalyticsEventEntity> findByTenantIdAndCreatedAtBetweenOrderByCreatedAtDesc(
            String tenantId, Instant start, Instant end);

    @Query("""
            select count(*)
            from analytics_events
            where tenant_id = :tenantId
              and event_type = :eventType
              and created_at between :start and :end
            """)
    Mono<Long> countByTenantIdAndEventTypeAndCreatedAtBetween(
            @Param("tenantId") String tenantId,
            @Param("eventType") String eventType,
            @Param("start") Instant start,
            @Param("end") Instant end);

    @Query("""
            select distinct event_type
            from analytics_events
            where tenant_id = :tenantId
            order by event_type
            """)
    Flux<String> findDistinctEventTypesByTenantId(@Param("tenantId") String tenantId);
}
