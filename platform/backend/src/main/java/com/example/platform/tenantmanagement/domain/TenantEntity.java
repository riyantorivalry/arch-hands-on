package com.example.platform.tenantmanagement.domain;

import com.example.platform.common.domain.AbstractAuditableEntity;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("tenants")
public class TenantEntity extends AbstractAuditableEntity {

    @Id
    @Column("tenant_id")
    private String tenantId;

    @Column("name")
    private String name;

    @Column("status")
    private TenantStatus status;

    @Column("plan_code")
    private String planCode;

    protected TenantEntity() {
    }

    public TenantEntity(String tenantId, String name, TenantStatus status, String planCode) {
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

    public TenantStatus getStatus() {
        return status;
    }

    public String getPlanCode() {
        return planCode;
    }

    @Override
    public Object getId() {
        return tenantId;
    }
}
