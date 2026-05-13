package com.example.platform.tenantmanagement.domain;

import com.example.platform.common.domain.AbstractAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "tenants")
public class TenantEntity extends AbstractAuditableEntity {

    @Id
    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "plan_code", nullable = false, length = 32)
    private String planCode;

    protected TenantEntity() {
    }

    public TenantEntity(String tenantId, String name, String status, String planCode) {
        this.tenantId = tenantId;
        this.name = name;
        this.status = status;
        this.planCode = planCode;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getName() {
        return name;
    }

    public String getStatus() {
        return status;
    }

    public String getPlanCode() {
        return planCode;
    }
}
