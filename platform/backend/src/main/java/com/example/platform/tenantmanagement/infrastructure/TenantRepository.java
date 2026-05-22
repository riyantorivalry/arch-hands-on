package com.example.platform.tenantmanagement.infrastructure;

import com.example.platform.tenantmanagement.domain.TenantEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface TenantRepository extends ReactiveCrudRepository<TenantEntity, String> {
}
