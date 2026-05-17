# Backend Comparison Implementation Plan

**Date:** 2026-05-17  
**Status:** Proposed  
**Purpose:** Turn the backend into a hands-on architecture comparison platform, not only a feature implementation.

## 1. Objective

The backend should support side-by-side comparison of multiple implementation strategies for the same business capability.

The goal is to learn architectural tradeoffs with evidence:

- latency and throughput
- consistency behavior
- operational complexity
- developer ergonomics
- scaling characteristics
- failure behavior
- cost and infrastructure overhead

This plan focuses on how to introduce comparison implementations without making the codebase chaotic.

## 2. Current Backend Baseline

The current backend is a Spring Boot modular monolith with these implemented or partially implemented areas:

- identity and access
- tenant management
- messaging
- documents
- tasks
- REST APIs
- GraphQL read API
- PostgreSQL persistence
- Flyway migrations
- observability stack
- in-process/domain event infrastructure
- transactional outbox scaffolding
- Redis scaffolding
- WebSocket scaffolding
- OpenSearch document search path with PostgreSQL fallback
- MongoDB analytics scaffolding

The baseline should be treated as **version 1 of the platform architecture**, even if some modules still need completion.

## 3. Comparison Strategy

Use **API versioning** when multiple implementations should be exposed and compared through public or benchmarkable API contracts.

Use **configuration-driven feature toggles** when the same API contract should stay stable, but the backend implementation can be swapped by application settings.

Use **Git branches** when the implementation changes deployment topology, infrastructure shape, or code ownership boundaries so much that side-by-side runtime comparison becomes impractical.

### Default Rule

Prefer API versioning for externally visible behavior differences.

Prefer feature toggles for internal strategy swaps behind the same contract.

Branches should be used only for experiments that are too invasive to coexist cleanly in one runnable application.

## 4. API Versioning vs Feature Toggles vs Branching

### API Versioning

Use versioning for capability-level comparisons where callers should be able to invoke each implementation directly.

Examples:

- document search using SQL `ILIKE`
- document search using PostgreSQL full-text search
- document search using `pg_trgm`
- document search using OpenSearch
- realtime updates using polling
- realtime updates using SSE
- realtime updates using WebSocket
- analytics using PostgreSQL JSONB
- analytics using MongoDB
- authorization using RBAC
- authorization using ABAC/OPA

Recommended URL format:

```text
/api/v1/...
/api/v2/...
/api/v3/...
```

For experimental versions:

```text
/api/experimental/...
```

Do not encode technology names directly in primary production-style URLs.

Good:

```text
GET /api/v1/workspaces/{workspaceId}/documents/search
GET /api/v2/workspaces/{workspaceId}/documents/search
GET /api/v3/workspaces/{workspaceId}/documents/search
```

Acceptable for internal benchmark endpoints:

```text
GET /api/benchmarks/search/postgres-ilike
GET /api/benchmarks/search/postgres-trgm
GET /api/benchmarks/search/opensearch
```

### Feature Toggles

Use feature toggles when the API should not change, but the backend strategy should be selected at runtime or deployment time.

This is useful for:

- event publishing mode
- cache strategy
- search backend behind one stable endpoint
- analytics storage target
- rate limiting algorithm
- authorization policy engine
- read routing strategy

Current reference pattern:

```yaml
platform:
  event-publishing:
    mode: outbox
    kafka:
      enabled: false
```

The event system already follows this style:

```text
platform.event-publishing.mode=in-process
platform.event-publishing.mode=outbox
platform.event-publishing.mode=kafka
```

Recommended generalized pattern:

```yaml
platform:
  feature:
    document-search:
      mode: postgres-ilike
    analytics:
      mode: postgres-jsonb
    realtime:
      mode: websocket
    authorization:
      mode: rbac
```

Example search modes:

```text
platform.feature.document-search.mode=postgres-ilike
platform.feature.document-search.mode=postgres-fts
platform.feature.document-search.mode=postgres-trgm
platform.feature.document-search.mode=opensearch
platform.feature.document-search.mode=semantic-vector
```

Example implementation style:

```java
@Component
@ConditionalOnProperty(
    name = "platform.feature.document-search.mode",
    havingValue = "opensearch"
)
public class OpenSearchDocumentSearch implements DocumentSearchUseCase {
}
```

Use feature toggles when you want to answer:

- What happens if production uses OpenSearch instead of PostgreSQL search?
- Can we switch back safely?
- Does the same endpoint behave acceptably with a cheaper implementation?
- What is the operational impact of enabling Kafka/outbox/Redis?

Feature toggles should be documented with:

- config key
- allowed values
- default value
- required infrastructure
- fallback behavior
- benchmark profile
- production readiness status

Feature toggles are not a replacement for API versioning. They are better for deployment-level strategy selection, while API versions are better for direct side-by-side client comparison.

### Branching

Use branches for architecture-level comparisons.

Examples:

- modular monolith vs extracted search service
- single Spring Boot deployable vs multiple services
- Kafka vs NATS as the platform event backbone
- REST-only backend vs gRPC service interfaces
- shared database schema vs tenant-per-schema model
- Kubernetes deployment model changes
- Terraform infrastructure changes

Recommended branch naming:

```text
experiment/search-service-extraction
experiment/kafka-event-backbone
experiment/nats-event-backbone
experiment/grpc-internal-api
experiment/tenant-schema-isolation
experiment/clickhouse-analytics
```

Branches must include benchmark notes before they are merged or abandoned.

## 5. Versioning Model

Each API version should map to a clear implementation strategy.

The same input shape should be preserved as much as possible so benchmarking is fair.

Each version should document:

- implementation approach
- storage dependencies
- consistency guarantees
- expected strengths
- expected weaknesses
- benchmark command
- metrics to compare
- rollback path

### Package Layout

Keep domain ownership stable. Put version differences at the API/application strategy boundary.

Recommended package shape:

```text
com.example.platform.documents
  api
    v1
    v2
    v3
  application
    search
      DocumentSearchUseCase.java
      PostgresIlikeDocumentSearch.java
      PostgresFullTextDocumentSearch.java
      PostgresTrgmDocumentSearch.java
      OpenSearchDocumentSearch.java
  domain
  infrastructure
```

Controller versions should be thin. The actual comparison should live behind strategy implementations.

## 5.1 Feature Toggle Model

For strategies selected by configuration, keep the public API stable and switch the injected implementation.

Recommended pattern:

```text
Controller
  -> Application facade
    -> Strategy interface
      -> Config-selected implementation
```

Example:

```text
DocumentsController
  -> DocumentsFacade
    -> DocumentSearchUseCase
      -> PostgresIlikeDocumentSearch
      -> PostgresFullTextDocumentSearch
      -> PostgresTrgmDocumentSearch
      -> OpenSearchDocumentSearch
```

Recommended configuration:

```yaml
platform:
  feature:
    document-search:
      mode: postgres-ilike
```

Recommended defaults:

- local development: simplest infrastructure, usually PostgreSQL
- benchmark profile: explicit mode per benchmark run
- production-like profile: most realistic target, for example OpenSearch or Kafka
- test profile: deterministic mode with minimal external dependencies

Example profiles:

```yaml
# application-local.yml
platform:
  feature:
    document-search:
      mode: postgres-ilike

# application-benchmark-opensearch.yml
platform:
  feature:
    document-search:
      mode: opensearch

# application-test.yml
platform:
  feature:
    document-search:
      mode: postgres-ilike
```

Use feature toggles for the implementation that backs the stable endpoint:

```text
GET /api/v1/workspaces/{workspaceId}/documents/search
```

Use API versioning when you want to expose all implementations at once:

```text
GET /api/v1/workspaces/{workspaceId}/documents/search
GET /api/v2/workspaces/{workspaceId}/documents/search
GET /api/v3/workspaces/{workspaceId}/documents/search
```

Both can coexist:

- `/api/v1` can mean stable product contract
- feature toggle chooses the implementation used by `/api/v1`
- `/api/benchmarks/search/*` can expose direct implementation-specific benchmark endpoints

## 6. Priority Comparison Tracks

## 6.1 Document Search

This should be the first comparison track because the project already has documents and OpenSearch scaffolding.

### v1: PostgreSQL ILIKE

Endpoint:

```text
GET /api/v1/workspaces/{workspaceId}/documents/search?query={query}
```

Implementation:

- use SQL `ILIKE`
- filter by `workspace_id`
- optionally filter by `tenant_id`
- order by `updated_at desc`
- add limit/offset

Purpose:

- simplest baseline
- low infrastructure cost
- works for small datasets

Tradeoffs:

- weak relevance ranking
- poor performance at scale without careful indexing
- limited language processing

### v2: PostgreSQL Full-Text Search and pg_trgm

Endpoint:

```text
GET /api/v2/workspaces/{workspaceId}/documents/search?query={query}
```

Implementation:

- add `tsvector` generated column or expression index
- use `to_tsvector`, `plainto_tsquery`, `websearch_to_tsquery`
- add `pg_trgm` extension
- compare exact full-text ranking vs fuzzy trigram matching

Purpose:

- evaluate how far PostgreSQL can go before adding a search engine

Tradeoffs:

- less operational overhead than OpenSearch
- strong transactional alignment
- relevance tuning is limited compared to search engines

### v3: OpenSearch

Endpoint:

```text
GET /api/v3/workspaces/{workspaceId}/documents/search?query={query}
```

Implementation:

- index documents into OpenSearch
- query title/content with `match` or `multi_match`
- filter by tenant/workspace
- support relevance score
- support index refresh/freshness metrics

Purpose:

- evaluate dedicated search infrastructure

Tradeoffs:

- better relevance and scale
- eventual consistency between Postgres and index
- more operational complexity

### v4: Semantic Search

Endpoint:

```text
GET /api/v4/workspaces/{workspaceId}/documents/semantic-search?query={query}
```

Implementation options:

- PostgreSQL + `pgvector`
- Qdrant
- OpenSearch vector search

Purpose:

- compare keyword search vs embedding-based retrieval

Tradeoffs:

- stronger conceptual matching
- requires embedding pipeline
- harder to explain and debug relevance

## 6.2 Realtime Delivery

Compare different ways to deliver updates to clients.

### v1: Polling

Endpoint:

```text
GET /api/v1/workspaces/{workspaceId}/events?since={cursor}
```

Purpose:

- simplest baseline
- works through standard HTTP

### v2: Server-Sent Events

Endpoint:

```text
GET /api/v2/workspaces/{workspaceId}/events/stream
```

Purpose:

- one-way realtime updates
- simpler than WebSocket
- useful for notifications and activity feeds

### v3: WebSocket

Endpoint:

```text
/ws/v3/realtime
```

Purpose:

- bidirectional realtime messaging
- suitable for collaborative workflows

Comparison metrics:

- connection count
- server memory per connection
- delivery latency
- reconnect behavior
- backpressure behavior
- load balancer complexity

## 6.3 Event Publishing

Event publishing should compare delivery guarantees.

This track should primarily use **feature toggles**, because the application APIs do not need to change when the event publishing mechanism changes.

Reference configuration:

```yaml
platform:
  event-publishing:
    mode: in-process
```

### v1: In-Process Events

Implementation:

- Spring `ApplicationEventPublisher`
- synchronous listeners

Good for:

- single-process modular monolith
- easy debugging

Weakness:

- no durability
- listener failure can affect request path

### v2: Transactional Outbox

Implementation:

- save domain event in `outbox_events`
- background processor publishes asynchronously
- retry with max attempts
- mark published after external publish succeeds

Good for:

- avoiding lost events
- preserving DB transaction boundary

Required fix:

- separate `OutboxDomainEventPublisher` from the external publisher used by the processor
- the processor must not publish back into the same outbox implementation

Example configuration:

```yaml
platform:
  event-publishing:
    mode: outbox
```

### v3: Kafka

Implementation:

- publish outbox events to Kafka topics
- add consumers for notifications, search indexing, analytics
- add dead-letter topic
- add idempotent consumer table

Good for:

- distributed event processing
- replayable event streams

Weakness:

- operational complexity
- schema governance required

Example configuration:

```yaml
platform:
  event-publishing:
    mode: kafka
    kafka:
      enabled: true
```

### v4: NATS

Implementation:

- compare NATS JetStream against Kafka

Good for:

- lower operational footprint
- simpler pub/sub and request/reply

Weakness:

- different ecosystem and retention model

## 6.4 Analytics Storage

### v1: PostgreSQL JSONB

Endpoint:

```text
GET /api/v1/tenants/{tenantId}/analytics/events
```

Good for:

- simple operational model
- transactional consistency
- small to medium analytics volume

### v2: MongoDB

Endpoint:

```text
GET /api/v2/tenants/{tenantId}/analytics/events
```

Good for:

- flexible event documents
- schema evolution experiments

### v3: ClickHouse

Endpoint:

```text
GET /api/v3/tenants/{tenantId}/analytics/events
```

Good for:

- high-volume analytical queries
- columnar aggregation workloads

Comparison metrics:

- ingestion throughput
- query latency by date range
- storage size
- schema evolution cost
- dashboard query performance

## 6.5 Authorization

### v1: RBAC

Current baseline:

- OWNER
- ADMIN
- MEMBER

### v2: ABAC

Implementation:

- evaluate attributes such as tenant plan, document status, ownership, risk level

### v3: OPA or Casbin

Implementation:

- externalize policy decisions
- keep business facts in application services

Comparison metrics:

- policy readability
- performance
- testability
- auditability
- ease of policy change

## 6.6 Protocol Comparison

### v1: REST

Current baseline.

Good for:

- public APIs
- simple CRUD and workflow operations

### v2: GraphQL

Current partial implementation.

Good for:

- read aggregation
- frontend-driven data shaping

Needed:

- authorization checks per resolver
- query depth/complexity limits
- pagination

### v3: gRPC

Use branch or internal-only module first:

```text
experiment/grpc-internal-api
```

Good for:

- service-to-service calls
- Protobuf contract comparison

Comparison metrics:

- payload size
- latency
- client generation ergonomics
- backward compatibility
- debugging complexity

## 7. Implementation Sequence

### Step 1: Stabilize Baseline

Before adding more alternatives, complete these backend correctness items:

- add `/api/v1` routes while keeping existing `/api` temporarily compatible
- persist workspace creation
- implement real workspace settings behavior
- add membership checks to all read paths
- add pagination to list/search endpoints
- document current known limitations
- fix outbox processor publisher separation

### Step 2: Create Search Comparison

Implement:

- `/api/v1/.../documents/search` using PostgreSQL `ILIKE`
- `/api/v2/.../documents/search` using PostgreSQL FTS and `pg_trgm`
- `/api/v3/.../documents/search` using OpenSearch

Add:

- shared response model
- shared benchmark dataset
- k6 benchmark script
- benchmark report in `docs/benchmarks`

### Step 3: Create Event Comparison

Implement:

- event mode `in-process`
- event mode `outbox`
- event mode `kafka`

Add:

- event contract documentation
- duplicate delivery test
- failed publish retry test
- consumer idempotency pattern

### Step 4: Create Realtime Comparison

Implement:

- polling endpoint
- SSE stream
- WebSocket stream

Add:

- connection benchmark
- delivery latency benchmark
- reconnect scenario

### Step 5: Create Analytics Comparison

Implement:

- PostgreSQL JSONB analytics repository
- MongoDB analytics repository
- optional ClickHouse analytics repository

Add:

- same query shape across versions
- ingestion benchmark
- dashboard query benchmark

### Step 6: Add Advanced Architecture Branches

Create branches only after the in-monolith comparisons are working.

Recommended branch order:

```text
experiment/search-service-extraction
experiment/kafka-event-backbone
experiment/grpc-internal-api
experiment/tenant-schema-isolation
experiment/clickhouse-analytics
```

## 8. Benchmark Requirements

Every comparison must include a benchmark report.

Report location:

```text
docs/benchmarks/BENCHMARK-YYYY-MM-DD-[topic].md
```

Each report must include:

- implementation versions compared
- dataset size
- environment
- test tool
- commands used
- p50 latency
- p95 latency
- p99 latency
- throughput
- error rate
- CPU and memory notes
- operational complexity notes
- recommendation

Suggested tools:

- k6 for HTTP benchmarks
- Gatling as an alternative JVM-friendly benchmark tool
- JMeter only if a GUI-driven comparison is useful
- Testcontainers for repeatable integration tests

## 9. Documentation Requirements

Each comparison track should produce:

- ADR for the decision
- benchmark report
- operational runbook
- known limitations
- fallback strategy

Recommended ADRs:

```text
ADR-005-api-versioning-strategy.md
ADR-006-document-search-comparison.md
ADR-007-event-publishing-strategy.md
ADR-008-realtime-delivery-strategy.md
ADR-009-analytics-storage-comparison.md
ADR-010-authorization-policy-strategy.md
```

## 10. Code Quality Rules

Do:

- keep controller versions thin
- share request/response contracts where possible
- isolate implementation differences behind interfaces
- keep tenant checks explicit
- keep benchmark data repeatable
- keep migration scripts reversible by adding new migrations, not editing old ones

Do not:

- mix several technology comparisons inside one implementation class
- hide business logic in generic utility classes
- make benchmarks depend on developer machine state
- compare implementations with different response shapes unless the difference is the point of the test
- branch for small alternatives that can coexist under API versions

## 11. Suggested Immediate Backlog

1. Create `ADR-005-api-versioning-strategy.md`.
2. Create `ADR-006-feature-toggle-comparison-strategy.md`.
3. Introduce `/api/v1` controllers for current REST behavior.
4. Keep old `/api` paths temporarily as compatibility aliases.
5. Extract document search behind `DocumentSearchUseCase`.
6. Add `platform.feature.document-search.mode`.
7. Implement `PostgresIlikeDocumentSearch`.
8. Implement `PostgresFullTextDocumentSearch`.
9. Move current OpenSearch implementation behind `OpenSearchDocumentSearch`.
10. Add pagination to all search/list endpoints.
11. Add k6 script for document search benchmark.
12. Write first search benchmark report.

## 12. Decision Summary

Use **API versioning** for implementation comparisons that callers should be able to invoke directly.

Use **feature toggles** for implementation comparisons that should keep the same API contract but swap backend strategy by configuration.

Use **branches** for topology or infrastructure changes that would make the mainline backend hard to run, test, or understand.

Recommended first comparison:

```text
Document Search
v1: PostgreSQL ILIKE
v2: PostgreSQL FTS + pg_trgm
v3: OpenSearch
v4: Semantic Search with pgvector or Qdrant
```

This gives the project an immediate senior-architect learning loop: same business capability, multiple implementations, measurable tradeoffs, and documented decisions.
