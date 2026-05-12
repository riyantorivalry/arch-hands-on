# ADR-003: Core Stack Selection

- Status: Accepted
- Date: 2026-05-13

## Context

The platform needs a primary stack that is credible for production SaaS systems, supports high concurrency, observability, security, and data-intensive workflows, and remains practical for staged evolution.

## Decision

Adopt the following baseline stack:

- **Backend language:** `Java`
- **Backend framework:** `Spring Boot`
- **Frontend:** `React`
- **Frontend framework:** `Next.js`
- **Primary database:** `PostgreSQL`
- **Cache / coordination:** `Redis`
- **Event backbone:** `Kafka`
- **AI / data workloads:** `Python`

## Rationale

### Java + Spring Boot

- strong ecosystem for production backend systems
- mature support for security, data access, observability, and messaging
- broad relevance for senior and principal backend architecture work
- good fit for modular monolith and service extraction patterns

### React + Next.js

- strong fit for realtime and mixed rendering modes
- supports SSR, streaming, and frontend architecture experiments
- broad ecosystem and production realism

### Python for AI / data workloads

- practical fit for embeddings, RAG pipelines, and experimentation in AI-adjacent services

## Consequences

### Positive

- Clear stack alignment for implementation
- Good separation between core platform backend and AI/data services
- Strong interview relevance for modern platform architecture

### Negative

- Polyglot complexity appears once Python AI services are introduced
- Java stack choices should remain disciplined to avoid framework bloat

## Follow-up

- Define Java project structure and module layout
- Choose Spring Boot support libraries deliberately
- Capture frontend rendering strategy in a later ADR
