# Backend Comparison Implementation Plan

**Date:** 2026-05-17
**Status:** Living implementation plan
**Purpose:** Turn the backend into a hands-on architecture comparison platform, not only a feature implementation.

## Current Implementation Status

As of 2026-05-18, the mainline backend implements these comparison tracks:

- Realtime delivery: polling (`/api/v1`), SSE (`/api/v2`), and WebSocket (`/ws/v3/realtime`)
- Document search: PostgreSQL `ILIKE` (`/api/v1`), PostgreSQL FTS + `pg_trgm` (`/api/v2`), and OpenSearch with PostgreSQL fallback (`/api/v3`)
- Analytics storage: PostgreSQL JSON/JSONB-style relational storage (`/api/v1`) and MongoDB (`/api/v2`)
- Authorization model: RBAC (`/api/v1`) and ABAC/OPA-style local evaluation (`/api/v2`)
- Authorization policy engine: `in-code`, `opa-local`, `casbin-local`, and seeded database policy rules (`db-policy`)
- Cache strategy: `caffeine`, `redis`, and `memcached`
- Rate limiting algorithm: `fixed-window`, `sliding-window`, and `token-bucket`
- PostgreSQL read routing: primary/replica routing with primary fallback

Still planned or branch-scoped:

- ClickHouse analytics
- external OPA or Casbin runtime integration
- Kafka/NATS event backbone
- search service extraction
- gRPC service-interface and tenant-per-schema topology experiments

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
- Redis-backed cache strategy
- Caffeine and Memcached cache strategies
- WebSocket realtime endpoint
- OpenSearch document search path with PostgreSQL fallback
- versioned document search comparison path
- MongoDB analytics comparison path
- authorization policy engine comparison path
- rate limiting algorithm comparison path
- PostgreSQL read routing

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

This is implemented on the mainline backend as a versioned comparison track.

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

### Policy Engine Comparison

Implementation:

- keep business facts in application services
- compare Java in-code rules, OPA-style local evaluation, Casbin-style local evaluation, and database-backed rules behind one stable authorization call
- defer external OPA or Casbin sidecars until the local policy shape and benchmark behavior are clear

For the in-process comparison track, keep the public application authorization call stable and select the engine by configuration:

```text
PLATFORM_FEATURE_AUTHORIZATION_ENGINE=in-code
PLATFORM_FEATURE_AUTHORIZATION_ENGINE=opa-local
PLATFORM_FEATURE_AUTHORIZATION_ENGINE=casbin-local
PLATFORM_FEATURE_AUTHORIZATION_ENGINE=db-policy
```

Use the versioned endpoints for policy-model comparison:

```text
POST /api/v1/workspaces/{workspaceId}/authorization/decisions
POST /api/v2/workspaces/{workspaceId}/authorization/decisions
```

Use benchmark endpoints for policy-engine comparison:

```text
GET  /api/benchmarks/authorization/engine
POST /api/benchmarks/authorization/{engine}/decisions
```

`db-policy` starts as a read-only seeded-policy engine using `authorization_policy_rules`. Runtime policy editing should be added only after the seeded rule behavior is benchmarked and audited.

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

## 6.7 Cache Strategy

Cache strategy should primarily use **feature toggles**, because product APIs should not change when cache infrastructure changes.

Reference configuration:

```yaml
platform:
  feature:
    cache:
      mode: caffeine
```

### Mode: Caffeine

Implementation:

- in-process Caffeine cache
- no external infrastructure
- deterministic local and test default

Good for:

- local development
- single-node deployments
- low operational overhead

Weakness:

- no cross-node sharing
- cache contents are lost on process restart

### Mode: Redis

Implementation:

- existing Spring `RedisTemplate`
- shared external cache
- TTL and atomic increment support

Good for:

- rate limiting across application nodes
- shared ephemeral data
- simple operational model when Redis already exists

Weakness:

- additional network dependency
- needs explicit outage and latency handling

### Mode: Memcached

Implementation:

- Memcached text protocol client
- simple distributed key/value cache
- no durable storage guarantees

Good for:

- lightweight distributed caching
- simple get/set workloads

Weakness:

- limited introspection
- no native key-pattern deletion
- TTL visibility is not exposed by the protocol

Benchmark endpoints:

```text
GET    /api/benchmarks/cache/strategy
POST   /api/benchmarks/cache/{strategy}/entries
GET    /api/benchmarks/cache/{strategy}/entries/{key}
POST   /api/benchmarks/cache/{strategy}/counters/{key}/increment
DELETE /api/benchmarks/cache/{strategy}/entries/{key}
```

Comparison metrics:

- get/set latency
- increment latency
- hit ratio
- memory usage
- external dependency failure behavior
- operational overhead

## 6.8 Rate Limiting Algorithm

Rate limiting should primarily use **feature toggles**, because product APIs should not change when the throttling algorithm changes.

Reference configuration:

```yaml
platform:
  feature:
    rate-limit:
      mode: fixed-window
```

### Mode: Fixed Window

Implementation:

- bucket requests by wall-clock window
- use one cache counter per key/window
- reject requests after the count exceeds the configured limit

Good for:

- simplest implementation
- low storage overhead
- distributed deployments when backed by Redis

Weakness:

- boundary bursts can exceed the intended rolling rate
- request distribution inside the window is not tracked

### Mode: Sliding Window

Implementation:

- store recent request timestamps per key
- remove timestamps outside the active window
- reject when the timestamp count reaches the configured limit

Good for:

- stricter rolling-window fairness
- easier reasoning for per-user API limits

Weakness:

- more memory per key
- needs atomic storage operations for production-grade distributed enforcement

### Mode: Token Bucket

Implementation:

- store token count and last refill timestamp per key
- refill tokens continuously based on `limit / windowSeconds`
- allow bursts up to the bucket capacity

Good for:

- burst-friendly API traffic
- smoother long-term throughput control

Weakness:

- less intuitive reset semantics
- needs careful clock and atomic update handling across nodes

Benchmark endpoints:

```text
GET  /api/benchmarks/rate-limit/algorithm
POST /api/benchmarks/rate-limit/{algorithm}/decisions
```

Comparison metrics:

- allowed/rejected decision latency
- burst behavior
- boundary behavior
- storage writes per decision
- memory usage per key
- distributed consistency when backed by Redis

## 7. Implementation Sequence

### Completed Mainline Tracks

- Realtime comparison: polling, SSE, and WebSocket
- Analytics storage comparison: PostgreSQL JSON/JSONB-style storage and MongoDB
- Authorization model comparison: RBAC and ABAC/OPA-style local evaluation
- Authorization policy engine comparison: in-code, OPA-style local, Casbin-style local, and database-backed seeded rules
- Cache strategy comparison: Caffeine, Redis, and Memcached
- Rate limiting algorithm comparison: fixed window, sliding window, and token bucket
- PostgreSQL read routing: primary/replica datasource routing with primary fallback

### Mainline Gaps

- Add benchmark reports for each implemented comparison track.
- Add ADRs for API versioning, feature-toggle comparison, realtime delivery, analytics storage, cache strategy, rate limiting, and authorization policy engines.
- Add operational runbooks for Redis/Memcached degradation, realtime connection storms, authorization policy rollback, and rate limit rollback.
- Improve document search benchmark datasets and capture comparison reports.
- Add ClickHouse only when high-volume analytical query benchmarks are ready.

### Branch-Scoped Experiments

Create or keep branches only when the comparison changes topology enough to make side-by-side runtime switching unclear.

Recommended branch candidates:

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

1. Create ADRs for the implemented comparison strategy: API versioning, feature toggles, realtime delivery, analytics storage, cache strategy, rate limiting, and authorization policy engines.
2. Add benchmark reports for realtime, analytics storage, cache strategy, rate limiting, and authorization policy engine tracks.
3. Add runbooks for Redis/Memcached degradation, realtime connection storms, authorization policy rollback, and rate limit rollback.
4. Extend the k6 comparison script with deeper workload mixes and persist benchmark reports for each run.
5. Decide whether document search remains the next mainline comparison or should move to a branch/service-extraction experiment.
6. Document known local infrastructure requirements for MongoDB, Redis, Memcached, and PostgreSQL replicas.

## 12. Decision Summary

Use **API versioning** for implementation comparisons that callers should be able to invoke directly.

Use **feature toggles** for implementation comparisons that should keep the same API contract but swap backend strategy by configuration.

Use **branches** for topology or infrastructure changes that would make the mainline backend hard to run, test, or understand.

Current mainline comparison surface:

```text
Realtime: polling, SSE, WebSocket
Analytics: PostgreSQL JSON/JSONB-style storage, MongoDB
Authorization model: RBAC, ABAC/OPA-style local
Authorization engine: in-code, OPA-style local, Casbin-style local, db-policy
Cache: Caffeine, Redis, Memcached
Rate limiting: fixed window, sliding window, token bucket
```

This gives the project an immediate senior-architect learning loop: same business capability, multiple implementations, measurable tradeoffs, and documented decisions.
