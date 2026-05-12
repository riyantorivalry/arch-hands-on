# ADR-001: Collaboration Platform Domain Choice

- Status: Accepted
- Date: 2026-05-13

## Context

The hands-on platform must expose principal-level architectural tradeoffs across real-time workloads, mixed consistency requirements, multi-tenancy, search, analytics, file handling, and AI-assisted workflows.

Candidate domains considered:

- multi-tenant e-commerce platform
- collaboration platform
- ride-hailing / delivery system
- AI agent platform

## Decision

Use a **collaboration platform** as the primary domain.

The product shape is a multi-tenant workspace system that combines:

- chat and threaded messaging
- collaborative documents
- tasks / tickets
- notifications
- file uploads
- search
- activity feeds
- analytics
- AI assistant workflows

## Rationale

This domain gives broad, credible coverage of:

- real-time communication
- streaming responses
- asynchronous event pipelines
- document and search workloads
- per-tenant isolation
- distributed notification patterns
- user-facing latency sensitivity
- eventual consistency tradeoffs

It also creates strong comparison cases for:

- REST vs GraphQL vs gRPC
- WebSocket vs SSE
- PostgreSQL vs MongoDB vs Redis vs search/vector systems
- modular monolith vs distributed service extraction

## Consequences

### Positive

- Strong alignment with modern SaaS architecture interviews
- Natural reason to implement realtime and mixed workloads
- Good surface area for both operational and product complexity

### Negative

- Scope can expand quickly if module boundaries are not controlled
- Realtime and collaboration features raise implementation complexity early

## Follow-up

- Define Phase 1 bounded contexts and feature cuts narrowly
- Keep the first implementation centered on tenant management, messaging, documents, and tasks
