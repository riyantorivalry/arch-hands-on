package com.example.platform.analytics.infrastructure;

import com.example.platform.analytics.domain.AnalyticsEventDocument;
import java.time.Instant;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.data.mongodb.repository.Query;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface AnalyticsEventMongoRepository extends ReactiveMongoRepository<AnalyticsEventDocument, String> {

    Flux<AnalyticsEventDocument> findByTenantIdAndCreatedAtBetweenOrderByCreatedAtDesc(
            String tenantId, Instant start, Instant end);

    Mono<Long> countByTenantIdAndEventTypeAndCreatedAtBetween(String tenantId, String eventType, Instant start, Instant end);

    @Query(value = "{ 'tenant_id': ?0 }", fields = "{ 'event_type': 1 }")
    Flux<AnalyticsEventDocument> findDistinctEventTypesByTenantId(String tenantId);
}

