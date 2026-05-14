# Architecture Workspace

This directory holds diagrams and design notes derived from the master plan.

## Phase 0: Foundation (Complete)

- [system-context.md](./system-context.md) - System boundaries and external actors
- [container-view.md](./container-view.md) - Container/deployment topology
- [bounded-contexts.md](./bounded-contexts.md) - Domain and module boundaries
- [phase-1-domain-model-outline.md](./phase-1-domain-model-outline.md) - Core entities and invariants
- [phase-1-module-contracts.md](./phase-1-module-contracts.md) - Module APIs and transaction rules

## Phase 1: Implementation Guidance

- [PHASE-1-IMPLEMENTATION-ROADMAP.md](./PHASE-1-IMPLEMENTATION-ROADMAP.md) **← IMPLEMENTATION CHECKLIST**
  - Implementation priority order
  - File structure checklist
  - Testing strategy
  - Success criteria for each module

- [PHASE-1b-EVENT-INFRASTRUCTURE.md](./PHASE-1b-EVENT-INFRASTRUCTURE.md) **← EVENT MODEL (just completed)**
  - Domain events architecture
  - In-process event publisher
  - 8 key events defined for Phase 1 workflows
  - Path to async events in Phase 2

## Future phases

- Phase 2: Realtime, Redis, and event backbone
  - sequence diagrams for key workflows
  - deployment topology with new services
  - event contracts specification
- Phase 3+: Search, files, analytics, AI foundations
- Phase 4+: Service extraction decisions
- Phase 5+: Observability and resilience hardening
