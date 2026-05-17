# Next Steps: Continue Phase 1 Backend Implementation

**Date:** 2026-05-14  
**Status:** Phase 0 complete, Phase 1 half-way through bootstrap

---

## What Just Completed ✅

1. **Documentation Templates** (5/14/2026)
   - Created `docs/benchmarks/BENCHMARK_TEMPLATE.md` - Ready for load testing
   - Created `docs/incidents/INCIDENT_TEMPLATE.md` - Ready for chaos experiments
   - Updated both README files with usage guidance

2. **Implementation Roadmap** (5/14/2026)
   - Created `docs/architecture/PHASE-1-IMPLEMENTATION-ROADMAP.md`
   - Detailed checklist of what to implement in each module
   - File structure requirements
   - Testing strategy
   - Success criteria

---

## Current Backend State

### ✅ Already Done

- Java 17 + Spring Boot 3.3.5 project configured
- Module structure in place (5 business modules)
- PostgreSQL schema created (3 Flyway migrations)
- Basic controllers and DTOs
- Comprehensive integration test suite
- Request context and authentication plumbing
- Basic domain entities

###  What's Partially Done

```
WORKING:
✓ LoginResponse controller returns token
✓ GET /me endpoint structure
✓ Database tables created
✓ Integration test suite runs

NEEDS COMPLETION:
⚠ Repositories not implemented
⚠ Application services incomplete
⚠ Authorization checks missing from some endpoints
⚠ Event publishing not connected
⚠ Tenant scoping in queries not verified
```

---

## The Next Step: Implement Repository Layer

### Why Start Here?

The integration tests are **already written** and expect these methods to work:
- `PlatformWorkflowIntegrationTests` in `src/test/java/com/example/platform/`

The tests call services that call repositories. Without repositories, services can't work.

### What to Do

Pick **one module** and implement it completely:

**Recommended: Start with `identity-access` module (simplest)**

1. Create the repository interfaces in `infrastructure/` package
2. Create Spring Data JPA repository implementations
3. Create repositories folder structure

Example:

```java
// src/main/java/com/example/platform/identityaccess/infrastructure/UserJpaRepository.java
@Repository
public interface UserJpaRepository extends JpaRepository<UserEntity, String> {
    Optional<UserEntity> findByEmail(String email);
}

// src/main/java/com/example/platform/identityaccess/infrastructure/MembershipJpaRepository.java
@Repository
public interface MembershipJpaRepository extends JpaRepository<MembershipEntity, Long> {
    Optional<MembershipEntity> findByWorkspaceIdAndUserId(String workspaceId, String userId);
    List<MembershipEntity> findByTenantIdAndWorkspaceId(String tenantId, String workspaceId);
}
```

4. Implement the domain repository interfaces
5. Wire into application services
6. Run integration tests to verify

### Module Implementation Order

Dependency order (implement in this sequence):

1. **Identity-Access** (no dependencies) ← START HERE
2. **Tenant-Management** (depends on Identity-Access)
3. **Messaging** (depends on both above)
4. **Documents** (depends on Identity + Tenant)
5. **Tasks** (depends on Identity + Tenant)

---

## File Locations You'll Need

### Backend src structure:
```
platform/backend/src/main/java/com/example/platform/
├── identityaccess/
│   ├── api/IdentityAccessController.java ✓
│   ├── application/
│   │   ├── IdentityAccessFacade.java
│   │   ├── AuthorizationService.java
│   │   └── SessionAuthenticationService.java
│   ├── domain/
│   │   ├── UserEntity.java ✓
│   │   ├── MembershipEntity.java ✓
│   │   ├── UserSessionEntity.java ✓
│   │   └── *Status.java enums ✓
│   └── infrastructure/ ← CREATE REPOS HERE
├── tenantmanagement/
├── messaging/
├── documents/
├── tasks/
└── common/
    ├── api/ (shared DTOs)
    ├── domain/ (shared base classes)
    ├── audit/ (audit logging)
    └── web/ (request context, exception handlers)
```

### Database resources:
```
platform/backend/src/main/resources/
└── db/migration/
    ├── V1__initial_schema.sql ✓ (Users, Memberships, Tenants, Workspaces)
    ├── V2__tasks_schema.sql ✓ (Tasks and Comments)
    └── V3__sessions_schema.sql ✓ (User Sessions)
```

### Tests:
```
platform/backend/src/test/java/com/example/platform/
├── PlatformApplicationTests.java (basic smoke test)
├── PlatformWorkflowIntegrationTests.java ← THIS ONE DRIVES IMPLEMENTATION
└── JsonFieldExtractor.java (test utility)
```

---

## How to Validate Your Work

### Run tests:
```powershell
cd D:\Project\arch-hands-on\platform\backend
mvn test
```

### Expected result:
```
[INFO] Tests run: 2, Failures: 0, Errors: 0
```

If `PlatformWorkflowIntegrationTests` fails, it means:
- A repository method is missing or
- A service is not calling the repository correctly or
- A controller is not injecting the service

### Debug workflow:
1. Find the failing test step
2. Trace back to controller that handles it
3. Trace to application service
4. Trace to repository interface that should exist
5. Create the repository implementation

---

## Architecture Decision: Repository Pattern

The code uses the **Repository pattern** (Domain-Driven Design):

```
API Layer (Controllers)
    ↓ calls
Application Layer (Services)
    ↓ calls
Domain Layer (Repository interfaces)
    ↓ implements
Infrastructure Layer (JPA repositories)
    ↓ persists to
Database (PostgreSQL)
```

This keeps domain logic **independent** of Spring Data JPA.

**Example implementation:**

```java
// Domain layer (what application services depend on)
public interface UserRepository {
    Optional<User> findById(String userId);
    User save(User user);
}

// Infrastructure layer (Spring Data)
@Repository
public class UserRepositoryImpl implements UserRepository {
    private final UserJpaRepository jpaRepository;
    
    public Optional<User> findById(String userId) {
        return jpaRepository.findById(userId)
            .map(this::toDomain);
    }
}
```

### Local Development

Database running via Docker Compose:

```powershell
cd D:\Project\arch-hands-on\platform\backend
docker-compose up -d
```

Or use Spring Boot's docker-compose starter (built-in).

Application will auto-run Flyway migrations on startup.

---

## Documentation Breadcrumbs

**If you get stuck:**

1. [Phase 1 Implementation Roadmap](../architecture/PHASE-1-IMPLEMENTATION-ROADMAP.md) ← COMPREHENSIVE GUIDE
2. [Phase 1 Module Contracts](../architecture/phase-1-module-contracts.md) - What APIs must exist
3. [Phase 1 Domain Model](../architecture/phase-1-domain-model-outline.md) - What entities are needed
4. [Module Contracts](../architecture/bounded-contexts.md) - Module boundaries and dependencies

---

## Command Reference

### Build and test locally:
```powershell
cd D:\Project\arch-hands-on\platform\backend

# Run all tests
mvn clean test

# Run only integration tests
mvn test -Dtest=PlatformWorkflowIntegrationTests

# Run specific test method
mvn test -Dtest=PlatformWorkflowIntegrationTests#createsTenantBootstrapsWorkspaceMembershipAndPostsMessage

# Build jar
mvn clean package

# Run locally (requires PostgreSQL)
mvn spring-boot:run
```

### Database:
```powershell
# Start Postgres
docker-compose up -d

# Check if running
docker ps

# Connect to DB
psql -h localhost -U postgres -d platform_local

# View tables
\dt

# Check migrations
SELECT * FROM flyway_schema_history;
```

---

## Success Looks Like

After completing identity-access module:

```
✓ mvn test passes completely
✓ UserEntity can be created and retrieved
✓ MembershipEntity associations work
✓ Login endpoint returns valid token
✓ GET /me works with authentication
✓ Authorization checks prevent unauthorized access
✓ Tenant data is isolated in queries
```

Then move to next module and repeat.

**Total time estimate for Phase 1:** 2-3 weeks with 1-2 developers

---

## Immediate Action Items

**RIGHT NOW (next 30 minutes):**

1. ✅ Read this file (done!)
2. Open `docs/architecture/PHASE-1-IMPLEMENTATION-ROADMAP.md`
3. Review Identity-Access section
4. Check current state of `identityaccess/infrastructure/` folder
5. Plan repository implementations

**NEXT (this session):**

1. Create `UserJpaRepository` interface
2. Create `MembershipJpaRepository` interface
3. Create `UserSessionJpaRepository` interface
4. Run `mvn test` to see what breaks
5. Fix failures one by one

**Session after:**

1. Complete application services for identity-access
2. Wire repositories into services
3. Add missing authorization checks
4. Run integration tests - verify they pass

---

## Escalation Points

**If stuck on:**

- **Spring Data JPA syntax**: Check phase-1-module-contracts for data access patterns
- **Entity relationships**: Review V1__initial_schema.sql for FK references
- **Test expectations**: Read PlatformWorkflowIntegrationTests to see what API should return
- **Authorization rules**: See bounded-contexts.md for RBAC requirements

**Next person to pick this up:**

- Pull this entire folder: `docs/architecture/`
- Start with `PHASE-1-IMPLEMENTATION-ROADMAP.md`
- Check test failures to understand what's missing
- Implement by module in dependency order

---

**Good luck! **

Questions? Reference the ADRs in `docs/adr/` for architecture decisions.
