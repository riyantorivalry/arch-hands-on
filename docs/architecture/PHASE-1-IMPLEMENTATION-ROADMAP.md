# Phase 1 Backend Implementation Roadmap

**Status:** In Progress  
**Last updated:** 2026-05-14

## Current State

### ✅ Completed

- Module structure (Java packages following DDD/hexagonal architecture)
- Database schema (Flyway migrations: roles, tenants, workspaces, messaging, documents, tasks)
- Basic controllers and DTOs
- Integration test suite defining expected behavior
- Request context and authentication plumbing

###  In Progress

- Domain entity implementations
- Application service implementations
- Repository and query interfaces
- Event publishing infrastructure

### ⏳ Not Yet Started

- Comprehensive error handling and validation
- Audit logging integration
- Correlation ID propagation
- Tenant scoping in queries
- Query optimization and pagination
- Feature flags infrastructure
- Background job framework
- Comprehensive metrics and monitoring

---

## Implementation Priority Order

### Phase 1a: Core Module Implementation (This Sprint)

Complete foundational modules that other modules depend on:

#### 1. **Identity-Access Module** (Foundation)
   - [ ] UserRepository and query methods
   - [ ] MembershipRepository with tenant scoping
   - [ ] UserSessionRepository with expiration
   - [ ] AuthorizationService implementation
   - [ ] SessionAuthenticationService implementation
   - [ ] Current actor resolution from token
   - [ ] Add login/logout integration tests
   - [ ] Add authorization checks integration tests

**Files to complete:**
- `src/main/java/com/example/platform/identityaccess/infrastructure/`
- Add UserRepository, MembershipRepository, UserSessionRepository interfaces

#### 2. **Tenant-Management Module** (Foundation)
   - [ ] TenantRepository and queries
   - [ ] WorkspaceRepository with tenant isolation
   - [ ] TenantBootstrapService (create tenant + workspace + initial user)
   - [ ] WorkspaceSettingsEntity and repository
   - [ ] Add create tenant integration test
   - [ ] Add workspace isolation validation tests

**Files to complete:**
- `src/main/java/com/example/platform/tenantmanagement/infrastructure/`
- Add TenantRepository, WorkspaceRepository interfaces
- Add TenantBootstrapService implementation

#### 3. **Messaging Module** (Business Logic)
   - [ ] ChannelRepository
   - [ ] MessageRepository with pagination
   - [ ] ThreadReplyEntity (if not already modeled)
   - [ ] MessagingApplicationService
   - [ ] Add channel creation tests
   - [ ] Add message posting tests
   - [ ] Add thread reply tests
   - [ ] Add authorization tests (who can create channels)

**Files to complete:**
- `src/main/java/com/example/platform/messaging/domain/` - ThreadEntity if needed
- `src/main/java/com/example/platform/messaging/infrastructure/`
- Add ChannelRepository, MessageRepository interfaces
- Add MessagingApplicationService

#### 4. **Documents Module** (Business Logic)
   - [ ] DocumentRepository
   - [ ] DocumentVersionEntity (for version history)
   - [ ] DocumentCommentRepository
   - [ ] DocumentApplicationService
   - [ ] Document status workflow validation
   - [ ] Add document create/edit tests
   - [ ] Add comment tests
   - [ ] Add authorization tests (only OWNER can edit)

**Files to complete:**
- `src/main/java/com/example/platform/documents/domain/` - DocumentVersionEntity
- `src/main/java/com/example/platform/documents/infrastructure/`
- Add DocumentRepository, DocumentCommentRepository interfaces
- Add DocumentApplicationService

#### 5. **Tasks Module** (Business Logic)
   - [ ] TaskRepository
   - [ ] TaskCommentRepository
   - [ ] TaskStatusPolicyValidator
   - [ ] TaskApplicationService
   - [ ] Task status transition validation
   - [ ] Add task create/update tests
   - [ ] Add comment tests
   - [ ] Add authorization tests (OWNER/MEMBER can assign differently)

**Files to complete:**
- `src/main/java/com/example/platform/tasks/domain/` - TaskStatusPolicyValidator
- `src/main/java/com/example/platform/tasks/infrastructure/`
- Add TaskRepository, TaskCommentRepository interfaces
- Add TaskApplicationService

---

### Phase 1b: Cross-Module Integration (Next Sprint)

#### Event Publishing
- [ ] Define domain events (TenantCreated, WorkspaceCreated, MessagePosted, etc.)
- [ ] Create ApplicationEventPublisher interface
- [ ] Implement in-memory event store for Phase 1
- [ ] Add event publishing to key domain operations
- [ ] Add event listener tests

#### Audit Logging
- [ ] Create AuditLogEntity
- [ ] Add audit table migration
- [ ] Create AuditLogPublisher
- [ ] Integrate into all write operations
- [ ] Add audit view/query endpoints

#### Tenant Scoping
- [ ] Add @TenantScoped annotation for queries
- [ ] Ensure all queries filter by tenant_id
- [ ] Add integration tests for tenant isolation
- [ ] Add cross-tenant query prevention tests

#### Request Correlation
- [ ] Add CorrelationId to RequestContext
- [ ] Propagate in all log statements
- [ ] Add to audit logs
- [ ] Add to event metadata

---

### Phase 1c: Observability & Non-Functional (Next 2 Sprints)

#### Metrics
- [ ] Add Micrometer/Prometheus metrics
- [ ] Track latency by operation and module
- [ ] Track error rates by operation
- [ ] Track database connection pool
- [ ] Add per-tenant load metrics

#### Error Handling
- [ ] Create comprehensive exception hierarchy
- [ ] Add @ExceptionHandler for each exception type
- [ ] Return consistent error format (code, message, trace ID)
- [ ] Add proper HTTP status codes
- [ ] Add validation error responses

#### Query Optimization
- [ ] Add pagination to all list endpoints
- [ ] Add database indexes for common queries
- [ ] Profile slow queries
- [ ] Add query caching where appropriate
- [ ] Document N+1 problems and solutions

---

## File Structure Checklist

For each module, ensure these files exist:

```text
com/example/platform/<module>/
├── api/
│   ├── <Module>Controller.java
│   └── <requests/responses>
├── application/
│   ├── <Module>ApplicationService.java
│   ├── <Module>Facade.java (if cross-module)
│   └── <domain-logic-services>
├── domain/
│   ├── *Entity.java (JPA entities)
│   ├── *Repository.java (interfaces only)
│   ├── *Status.java (enums)
│   ├── *Policy.java (business rules)
│   └── package-info.java (documentation)
├── infrastructure/
│   ├── *Repository.java (implementations)
│   ├── *Jpa.java (Spring Data JPA interfaces)
│   ├── <Module>DomainEventPublisher.java
│   └── package-info.java
└── [optional] event/
    └── *Event.java (domain events)
```

---

## Testing Strategy

### Integration Tests
- Currently: `PlatformWorkflowIntegrationTests` (comprehensive end-to-end)
- Add: module-specific integration tests
  - IdentityAccess: auth flows, membership
  - TenantManagement: bootstrap, isolation
  - Messaging: channels, messages, threads
  - Documents: creation, versioning, comments
  - Tasks: creation, status transitions, assignments

### Unit Tests
- Add: domain service tests
  - Status transition validation
  - Authorization checks
  - Query result mapping
  - Event generation

### Database Tests
- Add: repository contract tests
  - CRUD operations
  - Complex queries
  - Tenant scoping verification
  - Index effectiveness

---

## Known Limitations to Document

- [ ] Phase 1 uses synchronous request/response only (no async)
- [ ] No realtime updates (WebSocket/SSE) - Phase 2
- [ ] All events published in-process (no Kafka) - Phase 2
- [ ] No background jobs (all work is synchronous) - Phase 2
- [ ] Single database (no read replicas) - Phase 2+
- [ ] No caching layer (Redis) - Phase 2
- [ ] No search service (Elasticsearch) - Phase 3

---

## Database Performance Checklist

Before moving to Phase 2:

- [ ] All foreign keys created
- [ ] All recommended indexes created
- [ ] Query plans reviewed for seq scans
- [ ] Connection pool tuned
- [ ] Slow query logging enabled
- [ ] Table statistics current (ANALYZE)
- [ ] Dead tuples cleaned (VACUUM)

---

## API Surface Validation

Ensure each endpoint matches the module contracts:

**Identity-Access:**
- [ ] POST /api/auth/login
- [ ] POST /api/auth/logout
- [ ] GET /api/me
- [ ] GET /api/workspaces/{workspaceId}/memberships/me
- [ ] POST /api/workspaces/{workspaceId}/memberships

**Tenant-Management:**
- [ ] POST /api/tenants
- [ ] POST /api/tenants/{tenantId}/workspaces
- [ ] GET /api/workspaces/{workspaceId}
- [ ] PATCH /api/workspaces/{workspaceId}/settings

**Messaging:**
- [ ] POST /api/workspaces/{workspaceId}/channels
- [ ] GET /api/workspaces/{workspaceId}/channels
- [ ] POST /api/channels/{channelId}/messages
- [ ] GET /api/channels/{channelId}/messages
- [ ] POST /api/messages/{messageId}/replies

**Documents:**
- [ ] POST /api/workspaces/{workspaceId}/documents
- [ ] GET /api/workspaces/{workspaceId}/documents
- [ ] GET /api/documents/{documentId}
- [ ] PATCH /api/documents/{documentId}
- [ ] POST /api/documents/{documentId}/comments

**Tasks:**
- [ ] POST /api/workspaces/{workspaceId}/tasks
- [ ] GET /api/workspaces/{workspaceId}/tasks
- [ ] GET /api/tasks/{taskId}
- [ ] PATCH /api/tasks/{taskId}
- [ ] POST /api/tasks/{taskId}/comments

---

## Recommended Implementation Approach

1. **Pick one module** (suggest: Messaging, as it's independent)
2. **Complete all 5 layers** for that module:
   - API (Controller + DTOs)
   - Application (Service)
   - Domain (Entities + Rules)
   - Infrastructure (Repositories)
   - Tests (Happy path + error cases)
3. **Repeat for next module**, ensuring integration tests pass
4. **Cross-cut concerns** added incrementally (audit, events, metrics)
5. **Refactor shared patterns** as they emerge

---

## GitHub Actions / CI Expectations

Before merging:

- [ ] All tests pass (`mvn test`)
- [ ] Database migrations verified to work clean
- [ ] Code passes style checks
- [ ] No test coverage regression
- [ ] Integration tests exercise happy path + error cases
- [ ] New ADR if architectural decision made

---

## Success Criteria for Phase 1 Completion

- [ ] All endpoints in module contracts are implemented
- [ ] Integration test suite passes completely
- [ ] Database schema design review completed
- [ ] 80%+ code coverage
- [ ] All repositories proven to work with real schema
- [ ] Tenant isolation verified in integration tests
- [ ] Authentication/authorization working across all modules
- [ ] Error handling consistent across APIs
- [ ] Observability instrumentation in place (logging + metrics)
- [ ] Documentation updated with architectural decisions
- [ ] Baseline performance benchmarked

---

## Next Phase Gates

Phase 1 is "Done" when we can answer:

1. **Data model**: Are the entities and relationships correct for this domain?
2. **Isolation**: Can we verify tenant data is never leaked across tenants?
3. **Authorization**: Do RBAC rules work correctly across modules?
4. **Consistency**: Can we guarantee transactional correctness for complex workflows?
5. **Operability**: Can we trace a request through all modules and see what happened?
6. **Performance**: What is the baseline latency, throughput, and resource usage?

Only then proceed to Phase 2 (Realtime + Redis + Events).

---

## Feedback Loop

As implementation progresses:

- [ ] Update ADRs with implementation discovery
- [ ] Document any deviations from contracts
- [ ] Capture integration challenges
- [ ] Note performance hotspots
- [ ] Identify missing observability requirements
