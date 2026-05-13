# Backend

This is the Phase 1 Java modular monolith scaffold.

## Stack

- Java 21
- Spring Boot
- Spring Web
- Spring Data JPA
- PostgreSQL
- Actuator

## Module layout

Each business module follows the package pattern defined in the architecture docs:

```text
com.example.platform.<module>
  api
  application
  domain
  infrastructure
```

Current modules:

- `identityaccess`
- `tenantmanagement`
- `messaging`
- `documents`
- `tasks`

## Current scope

This scaffold establishes:

- one deployable Spring Boot app
- tenant-aware request context plumbing
- explicit module packages
- placeholder application services and controllers
- health and platform info endpoints

Feature persistence and domain rules should be added module by module from the Phase 1 contracts.
