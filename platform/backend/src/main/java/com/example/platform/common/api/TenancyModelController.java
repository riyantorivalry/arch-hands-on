package com.example.platform.common.api;

import com.example.platform.common.infrastructure.database.TenantSchemaProvisioner;
import com.example.platform.common.web.RequestContexts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/benchmarks/tenancy")
public class TenancyModelController {

    private final TenantSchemaProvisioner tenantSchemaProvisioner;
    private final String mode;
    private final String defaultSchema;

    public TenancyModelController(
            TenantSchemaProvisioner tenantSchemaProvisioner,
            @Value("${platform.tenancy.mode:shared-schema}") String mode,
            @Value("${platform.tenancy.tenant-schema.default-schema:public}") String defaultSchema
    ) {
        this.tenantSchemaProvisioner = tenantSchemaProvisioner;
        this.mode = mode;
        this.defaultSchema = defaultSchema;
    }

    @GetMapping("/model")
    public TenancyModelResponse model() {
        RequestContexts.authenticated();
        return new TenancyModelResponse(mode, defaultSchema, tenantSchemaProvisioner.isTenantSchemaMode());
    }

    @GetMapping("/tenants/{tenantId}/schema")
    public TenantSchemaResponse schema(@PathVariable String tenantId) {
        RequestContexts.authenticated();
        return new TenantSchemaResponse(
                tenantId,
                tenantSchemaProvisioner.schemaForTenant(tenantId),
                tenantSchemaProvisioner.isTenantSchemaMode()
        );
    }

    public record TenancyModelResponse(String mode, String defaultSchema, boolean tenantSchemaRoutingEnabled) {
    }

    public record TenantSchemaResponse(String tenantId, String schemaName, boolean provisionedWhenTenantCreated) {
    }
}
