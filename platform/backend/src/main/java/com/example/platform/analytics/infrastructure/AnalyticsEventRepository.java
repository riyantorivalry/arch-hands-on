package com.example.platform.analytics.infrastructure;

import com.example.platform.analytics.domain.AnalyticsEventEntity;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AnalyticsEventRepository extends JpaRepository<AnalyticsEventEntity, String> {

    List<AnalyticsEventEntity> findByTenantIdAndCreatedAtBetweenOrderByCreatedAtDesc(
            String tenantId, Instant start, Instant end);

    @Query("SELECT COUNT(a) FROM AnalyticsEventEntity a WHERE a.tenantId = :tenantId AND a.eventType = :eventType AND a.createdAt BETWEEN :start AND :end")
    long countByTenantIdAndEventTypeAndCreatedAtBetween(
            @Param("tenantId") String tenantId,
            @Param("eventType") String eventType,
            @Param("start") Instant start,
            @Param("end") Instant end);

    @Query("SELECT DISTINCT a.eventType FROM AnalyticsEventEntity a WHERE a.tenantId = :tenantId ORDER BY a.eventType")
    List<String> findDistinctEventTypesByTenantId(@Param("tenantId") String tenantId);
}
