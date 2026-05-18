# Architecture Workspace

This directory holds the platform's system views, bounded-context notes, and backend comparison strategy.

## Core Architecture

- [system-context.md](./system-context.md) - system boundaries and external actors
- [container-view.md](./container-view.md) - container and deployment topology
- [bounded-contexts.md](./bounded-contexts.md) - domain and module boundaries
- [phase-1-domain-model-outline.md](./phase-1-domain-model-outline.md) - core entities and invariants
- [phase-1-module-contracts.md](./phase-1-module-contracts.md) - module APIs and transaction rules

## Comparison Architecture

- [backend-comparison-implementation-plan.md](./backend-comparison-implementation-plan.md) - how API versioning, feature toggles, and branch experiments are used

Current mainline comparison tracks include realtime delivery, analytics storage, authorization models, authorization policy engines, cache strategies, rate limiting algorithms, and PostgreSQL read routing. Topology-changing experiments such as gRPC service interfaces and tenant-per-schema isolation should stay documented on their experiment branches until merged.

## Future Architecture Notes

Add new architecture documents here when a decision changes the module boundary, deployment topology, persistence model, or public contract. Use ADRs in [../adr/](../adr/) for durable decisions and this directory for working design notes.
