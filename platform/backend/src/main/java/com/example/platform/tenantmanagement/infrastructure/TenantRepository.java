package com.example.platform.tenantmanagement.infrastructure;

import com.example.platform.tenantmanagement.domain.TenantEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantRepository extends JpaRepository<TenantEntity, String> {
}
