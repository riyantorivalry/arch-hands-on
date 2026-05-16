package com.example.platform.analytics.infrastructure;

import com.example.platform.analytics.domain.AnalyticsEventDocument;
import java.time.Instant;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface AnalyticsEventMongoRepository extends MongoRepository<AnalyticsEventDocument, String> {

    List<AnalyticsEventDocument> findByTenantIdAndCreatedAtBetweenOrderByCreatedAtDesc(
            String tenantId, Instant start, Instant end);

    long countByTenantIdAndEventTypeAndCreatedAtBetween(String tenantId, String eventType, Instant start, Instant end);

    @Query(value = "{ 'tenant_id': ?0 }", fields = "{ 'event_type': 1 }")
    List<AnalyticsEventDocument> findDistinctEventTypesByTenantId(String tenantId);
}

