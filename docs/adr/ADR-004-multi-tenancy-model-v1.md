# ADR-004: Multi-Tenancy Model V1

- Status: Accepted
- Date: 2026-05-13

## Context

The platform must be multi-tenant and later compare isolation strategies. Phase 1 needs a model that is operationally simple enough to implement quickly while still preserving clear tenant scoping and future migration options.

## Decision

Start with **shared database, shared schema** using strict tenant scoping in the application and schema design.

Planned future comparison targets:

1. shared database with stronger tenant partitioning patterns
2. hybrid isolation for premium or regulated tenants

## Rationale

- fastest route to a working Phase 1 system
- easiest baseline for comparing cost and complexity against stronger isolation models
- fits the modular monolith stage well
- keeps initial infra footprint controlled

## Guardrails

- every tenant-owned table must include `tenant_id`
- all access paths must enforce tenant scoping
- auditing must record tenant context
- tests must cover cross-tenant access isolation
- schema design should avoid blocking later migration to hybrid isolation

## Consequences

### Positive

- low operational overhead
- lower cost
- faster early development

### Negative

- weaker isolation than tenant-dedicated infrastructure
- higher risk of noisy-neighbor effects
- future migration work required for premium isolation

## Follow-up

- define tenancy rules in domain and API contracts
- document migration path to hybrid model
- add tenant-isolation checks to integration tests
