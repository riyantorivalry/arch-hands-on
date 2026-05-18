# Documentation Index

This directory is the working architecture and production-engineering record for the platform defined in [../PRINCIPAL_ENGINEER_HANDS_ON_PLATFORM_PLAN.md](../PRINCIPAL_ENGINEER_HANDS_ON_PLATFORM_PLAN.md).

## Start Here

- [api/openapi.yaml](./api/openapi.yaml) - current REST, realtime, and benchmark API contract
- [architecture/backend-comparison-implementation-plan.md](./architecture/backend-comparison-implementation-plan.md) - comparison strategy and implementation map
- [architecture/README.md](./architecture/README.md) - architecture document index
- [benchmarks/README.md](./benchmarks/README.md) - benchmark report guidance
- [runbooks/README.md](./runbooks/README.md) - operational procedures

## Current Backend Comparison Tracks

The backend now behaves as a comparison platform. The mainline application supports these implemented tracks:

- Realtime delivery: polling (`/api/v1`), SSE (`/api/v2`), and WebSocket (`/ws/v3/realtime`)
- Analytics storage: PostgreSQL JSON/JSONB-style relational storage (`/api/v1`) and MongoDB document storage (`/api/v2`)
- Authorization model: RBAC (`/api/v1`) and ABAC/OPA-style local evaluation (`/api/v2`)
- Authorization policy engine: `in-code`, `opa-local`, `casbin-local`, and seeded database policy rules (`db-policy`)
- Cache strategy: `caffeine`, `redis`, and `memcached`
- Rate limiting algorithm: `fixed-window`, `sliding-window`, and `token-bucket`
- Read routing: PostgreSQL primary/replica datasource routing with primary fallback when no replica is configured

Branch-level experiments, such as gRPC service interfaces or tenant-per-schema isolation, should be documented on their branch because they change topology and code ownership more heavily than runtime strategy switches.

## Structure

- [adr/](./adr/) - architecture decision records
- [architecture/](./architecture/) - diagrams, system views, bounded contexts, and comparison plans
- [api/](./api/) - OpenAPI contract for implemented HTTP and realtime entry points
- [benchmarks/](./benchmarks/) - load test plans, raw outputs, and performance reports
- [incidents/](./incidents/) - simulated incidents and postmortems
- [runbooks/](./runbooks/) - operational procedures
- [chaos/](./chaos/) - failure-injection plans and experiment results
- [scalability/](./scalability/) - scaling strategy, capacity analysis, and cost notes

## Baseline Decisions

- Primary backend language: `Java`
- Backend baseline framework: `Spring Boot`
- Frontend baseline: `React`
- Frontend framework baseline: `Next.js`
- Primary domain: collaboration platform
- Architecture strategy: modular monolith first, then selective service extraction

## Documentation Maintenance

Keep [api/openapi.yaml](./api/openapi.yaml), [platform/backend/README.md](../platform/backend/README.md), and the comparison plan aligned whenever a new comparison track is implemented. If a comparison requires a dedicated branch, document it on that branch and link it back only after it is merged or intentionally preserved as a long-running experiment.
