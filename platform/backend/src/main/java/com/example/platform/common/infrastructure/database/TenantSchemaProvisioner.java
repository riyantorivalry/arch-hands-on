package com.example.platform.common.infrastructure.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class TenantSchemaProvisioner {

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;
    private final TenantSchemaNameResolver schemaNameResolver;
    private final String mode;
    private final String defaultSchema;
    private final String[] migrationLocations;

    public TenantSchemaProvisioner(
            DataSource dataSource,
            TenantSchemaNameResolver schemaNameResolver,
            @Value("${platform.tenancy.mode:shared-schema}") String mode,
            @Value("${platform.tenancy.tenant-schema.default-schema:public}") String defaultSchema,
            @Value("${spring.flyway.locations:classpath:db/migration}") String migrationLocations
    ) {
        this.dataSource = dataSource;
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.schemaNameResolver = schemaNameResolver;
        this.mode = mode;
        this.defaultSchema = defaultSchema;
        this.migrationLocations = Arrays.stream(migrationLocations.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toArray(String[]::new);
    }

    public boolean isTenantSchemaMode() {
        return "tenant-schema".equalsIgnoreCase(mode);
    }

    public String schemaForTenant(String tenantId) {
        return schemaNameResolver.schemaForTenant(tenantId);
    }

    public void provisionTenant(String tenantId, String workspaceId, String ownerUserId) {
        if (!isTenantSchemaMode()) {
            return;
        }

        String tenantSchema = schemaForTenant(tenantId);
        createSchema(tenantSchema);
        migrateSchema(tenantSchema);
        copyBootstrapRows(tenantSchema, tenantId, workspaceId, ownerUserId);
        registerSchema(tenantId, tenantSchema);
    }

    private void createSchema(String schemaName) {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("create schema if not exists " + schemaName);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to create tenant schema " + schemaName, exception);
        }
    }

    private void migrateSchema(String schemaName) {
        Flyway.configure()
                .dataSource(dataSource)
                .schemas(schemaName)
                .defaultSchema(schemaName)
                .locations(migrationLocations)
                .load()
                .migrate();
    }

    private void copyBootstrapRows(String tenantSchema, String tenantId, String workspaceId, String ownerUserId) {
        copyTenant(tenantSchema, tenantId);
        copyWorkspace(tenantSchema, workspaceId);
        copyUser(tenantSchema, ownerUserId);
        copyMembership(tenantSchema, workspaceId, ownerUserId);
        copyWorkspaceSettings(tenantSchema, workspaceId);
    }

    private void copyTenant(String tenantSchema, String tenantId) {
        jdbcTemplate.update("""
                insert into %s.tenants (tenant_id, name, status, plan_code, created_at, updated_at)
                select tenant_id, name, status, plan_code, created_at, updated_at
                from %s.tenants source
                where source.tenant_id = ?
                  and not exists (select 1 from %s.tenants target where target.tenant_id = source.tenant_id)
                """.formatted(tenantSchema, defaultSchema, tenantSchema), tenantId);
    }

    private void copyWorkspace(String tenantSchema, String workspaceId) {
        jdbcTemplate.update("""
                insert into %s.workspaces (workspace_id, tenant_id, name, status, created_at, updated_at)
                select workspace_id, tenant_id, name, status, created_at, updated_at
                from %s.workspaces source
                where source.workspace_id = ?
                  and not exists (select 1 from %s.workspaces target where target.workspace_id = source.workspace_id)
                """.formatted(tenantSchema, defaultSchema, tenantSchema), workspaceId);
    }

    private void copyUser(String tenantSchema, String ownerUserId) {
        jdbcTemplate.update("""
                insert into %s.users (user_id, email, display_name, status, created_at, updated_at)
                select user_id, email, display_name, status, created_at, updated_at
                from %s.users source
                where source.user_id = ?
                  and not exists (select 1 from %s.users target where target.user_id = source.user_id)
                """.formatted(tenantSchema, defaultSchema, tenantSchema), ownerUserId);
    }

    private void copyMembership(String tenantSchema, String workspaceId, String ownerUserId) {
        jdbcTemplate.update("""
                insert into %s.workspace_memberships (tenant_id, workspace_id, user_id, role, status, created_at, updated_at)
                select tenant_id, workspace_id, user_id, role, status, created_at, updated_at
                from %s.workspace_memberships source
                where source.workspace_id = ? and source.user_id = ?
                  and not exists (
                      select 1 from %s.workspace_memberships target
                      where target.workspace_id = source.workspace_id and target.user_id = source.user_id
                  )
                """.formatted(tenantSchema, defaultSchema, tenantSchema), workspaceId, ownerUserId);
    }

    private void copyWorkspaceSettings(String tenantSchema, String workspaceId) {
        jdbcTemplate.update("""
                insert into %s.workspace_settings (
                    workspace_id,
                    tenant_id,
                    default_document_status,
                    task_auto_assign_enabled,
                    message_retention_days,
                    created_at,
                    updated_at
                )
                select workspace_id,
                       tenant_id,
                       default_document_status,
                       task_auto_assign_enabled,
                       message_retention_days,
                       created_at,
                       updated_at
                from %s.workspace_settings source
                where source.workspace_id = ?
                  and not exists (
                      select 1 from %s.workspace_settings target
                      where target.workspace_id = source.workspace_id
                  )
                """.formatted(tenantSchema, defaultSchema, tenantSchema), workspaceId);
    }

    private void registerSchema(String tenantId, String tenantSchema) {
        jdbcTemplate.update("""
                insert into %s.tenant_schema_registry (tenant_id, schema_name, model, provisioned_at)
                select ?, ?, 'tenant-schema', current_timestamp
                where not exists (
                    select 1 from %s.tenant_schema_registry registry where registry.tenant_id = ?
                )
                """.formatted(defaultSchema, defaultSchema), tenantId, tenantSchema, tenantId);
    }
}
