# ADR-002: Monolith-First Evolution Strategy

- Status: Accepted
- Date: 2026-05-13

## Context

The project goal is to learn architectural tradeoffs, not to maximize technology count on day one. Starting with distributed services immediately would add deployment, tracing, networking, and debugging complexity before core domain boundaries are understood.

## Decision

Start with a **modular monolith** and evolve to extracted services only when the project can justify the split with clear scaling, ownership, or operational evidence.

## Rationale

This approach allows the project to:

- validate domain boundaries before network boundaries
- build transactional workflows faster
- preserve refactoring speed in early phases
- introduce observability and benchmarking before service sprawl
- create a meaningful before/after comparison for extraction decisions

## Extraction criteria

Candidate modules may be extracted when at least one of these becomes true:

- independent scaling is materially useful
- operational isolation reduces blast radius
- technology specialization is justified
- deployment cadence differs significantly
- runtime characteristics diverge from the monolith

## Consequences

### Positive

- Lower initial complexity
- Better learning signal around service boundaries
- Stronger comparison between topology choices

### Negative

- Some later extractions will require refactoring
- Early module discipline must be enforced to avoid a tangled monolith

## Follow-up

- Define bounded contexts and module contracts in Phase 1
- Keep inter-module integration explicit
- Track extraction triggers in later ADRs
