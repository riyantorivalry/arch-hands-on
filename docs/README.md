# Documentation Index

This directory is the working architecture and production-engineering record for the platform defined in [../PRINCIPAL_ENGINEER_HANDS_ON_PLATFORM_PLAN.md](../PRINCIPAL_ENGINEER_HANDS_ON_PLATFORM_PLAN.md).

## Structure

- [adr/](./adr/) - architecture decision records
- [architecture/](./architecture/) - diagrams, system views, and bounded context definitions
- [benchmarks/](./benchmarks/) - load tests, protocol comparisons, and performance reports
- [incidents/](./incidents/) - postmortems and incident simulation writeups
- [runbooks/](./runbooks/) - operational procedures
- [chaos/](./chaos/) - failure-injection plans and experiment results
- [scalability/](./scalability/) - scaling strategy, cost analysis, and regional expansion notes

## Current baseline decisions

- Primary backend language: `Java`
- Backend baseline framework: `Spring Boot`
- Frontend baseline: `React`
- Frontend framework baseline: `Next.js`
- Primary domain: collaboration platform
- Architecture strategy: modular monolith first, then selective service extraction

## Immediate documentation sequence

1. Finalize domain and bounded contexts
2. Expand ADRs as decisions are made
3. Add system context and container diagrams
4. Define Phase 1 module contracts
5. Add benchmark and incident templates before implementation work accelerates
