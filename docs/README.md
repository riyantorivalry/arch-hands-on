# Documentation Index

This directory is the working architecture and production-engineering record for the platform defined in [../PRINCIPAL_ENGINEER_HANDS_ON_PLATFORM_PLAN.md](../PRINCIPAL_ENGINEER_HANDS_ON_PLATFORM_PLAN.md).

##  Start Here

**[NEXT_STEPS.md](./NEXT_STEPS.md)** - What to do next in Phase 1 backend implementation  
**[SESSION-2-SUMMARY.md](./SESSION-2-SUMMARY.md)** - Latest work: Event infrastructure complete  
**[architecture/PHASE-1-PROGRESS-REPORT.md](./architecture/PHASE-1-PROGRESS-REPORT.md)** - What's done, what's pending  

## Structure

- [adr/](./adr/) - architecture decision records
- [architecture/](./architecture/) - diagrams, system views, and bounded context definitions
  - **→ [PHASE-1-PROGRESS-REPORT.md](./architecture/PHASE-1-PROGRESS-REPORT.md)** Phase 1 status (Phase 1a complete, Phase 1b infrastructure done, Phase 1c next)
  - **→ [PHASE-1-IMPLEMENTATION-ROADMAP.md](./architecture/PHASE-1-IMPLEMENTATION-ROADMAP.md)** detailed implementation checklist
  - **→ [PHASE-1b-EVENT-INFRASTRUCTURE.md](./architecture/PHASE-1b-EVENT-INFRASTRUCTURE.md)** event publishing system just created
- [benchmarks/](./benchmarks/) - load tests, protocol comparisons, and performance reports
  - See [BENCHMARK_TEMPLATE.md](./benchmarks/BENCHMARK_TEMPLATE.md) for report structure
- [incidents/](./incidents/) - postmortems and incident simulation writeups  
  - See [INCIDENT_TEMPLATE.md](./incidents/INCIDENT_TEMPLATE.md) for report structure
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
