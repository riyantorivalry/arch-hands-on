# Tenant Schema Isolation Branch

**Branch:** `experiment/tenant-schema-isolation`  
**Base:** `main`  
**Status:** Experimental  
**Purpose:** Compare the shared database schema baseline with a tenant-per-schema model.

## Baseline

The default mode keeps the main branch model:

```text
PLATFORM_TENANCY_MODE=shared-schema
```

This stores all tenants in the same schema and relies on:

- `tenant_id` columns
- tenant/workspace membership checks
- tenant-scoped indexes
- application-level query discipline

## Experiment

Enable tenant schema isolation with:

```text
PLATFORM_TENANCY_MODE=tenant-schema
PLATFORM_TENANCY_DEFAULT_SCHEMA=public
PLATFORM_TENANCY_SCHEMA_PREFIX=tenant_
```

In this mode:

- tenant bootstrap creates a deterministic tenant schema
- Flyway migrates that schema
- bootstrap tenant/workspace/user/membership/settings rows are copied into the tenant schema
- authenticated request database connections switch to the tenant schema using the request tenant context

## Comparison Endpoints

```text
GET /api/benchmarks/tenancy/model
GET /api/benchmarks/tenancy/tenants/{tenantId}/schema
```

## Known Limits

This branch is an experiment, not a production tenancy cutover. The next hardening steps are:

- tenant-schema integration tests against PostgreSQL, not only H2 shared-schema tests
- explicit handling for tenant-level migrations after tenants already exist
- operational scripts for backup, restore, and schema inventory
- stronger separation of global control-plane tables from tenant-local tables
