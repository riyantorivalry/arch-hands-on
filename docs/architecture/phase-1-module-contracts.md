# Phase 1 Module Contracts

## Purpose

This document turns the bounded contexts into implementation contracts for the Java modular monolith.

The goal is to prevent Phase 1 from collapsing into a layered monolith with shared tables and informal coupling.

## Architectural shape

The monolith should be:

- one deployable
- one primary PostgreSQL database
- multiple internal business modules
- explicit module APIs
- isolated domain ownership per module

## Module dependency policy

Allowed dependency direction:

1. `identity-access`
2. `tenant-management`
3. `messaging`
4. `documents`
5. `tasks`

Interpretation:

- lower-level foundational modules can be depended on by higher-level business modules
- business modules at the same level should not depend on each other's internal code
- integration between peer modules should happen through exported interfaces only

## Exported contract examples

### identity-access

Exports:

- resolve current actor
- fetch user by id
- fetch workspace membership
- fetch actor roles in workspace

### tenant-management

Exports:

- fetch tenant by id
- fetch workspace by id
- validate workspace belongs to tenant
- read tenant/workspace status

### messaging

Exports:

- fetch channel by id
- fetch message by id
- list recent messages for workspace/channel

### documents

Exports:

- fetch document by id
- list documents by workspace

### tasks

Exports:

- fetch task by id
- list tasks by workspace / assignee / status

## Transaction rules

- each write use case should have one owning application service
- a transaction should stay inside a single module whenever possible
- cross-module workflows in Phase 1 should prefer synchronous orchestration over distributed patterns
- events may be published after commit for future consumers, but Phase 1 should not depend on asynchronous completion for core correctness

## Persistence rules

- every module owns its tables
- cross-module foreign keys are allowed only where operationally justified
- other modules must not write directly into tables they do not own
- read-side joins across modules should be isolated to query services or projections

## Event model for later phases

Phase 1 should already emit internal domain events for future evolution.

Initial candidate events:

- `TenantCreated`
- `WorkspaceCreated`
- `WorkspaceMemberAdded`
- `ChannelCreated`
- `MessagePosted`
- `DocumentCreated`
- `DocumentUpdated`
- `TaskCreated`
- `TaskAssigned`
- `TaskStatusChanged`

## Minimum API surface by module

### identity-access

- `POST /auth/login`
- `POST /auth/logout`
- `GET /me`
- `GET /workspaces/{workspaceId}/memberships/me`

### tenant-management

- `POST /tenants`
- `POST /tenants/{tenantId}/workspaces`
- `GET /workspaces/{workspaceId}`
- `PATCH /workspaces/{workspaceId}/settings`

### messaging

- `POST /workspaces/{workspaceId}/channels`
- `GET /workspaces/{workspaceId}/channels`
- `POST /channels/{channelId}/messages`
- `GET /channels/{channelId}/messages`
- `POST /messages/{messageId}/replies`

### documents

- `POST /workspaces/{workspaceId}/documents`
- `GET /workspaces/{workspaceId}/documents`
- `GET /documents/{documentId}`
- `PATCH /documents/{documentId}`
- `POST /documents/{documentId}/comments`

### tasks

- `POST /workspaces/{workspaceId}/tasks`
- `GET /workspaces/{workspaceId}/tasks`
- `GET /tasks/{taskId}`
- `PATCH /tasks/{taskId}`
- `POST /tasks/{taskId}/comments`

## Initial aggregate rules

### Messaging

- a message belongs to exactly one channel
- a thread reply belongs to exactly one parent message
- a user cannot post without workspace membership

### Documents

- a document belongs to exactly one workspace
- every update must preserve an auditable modifier and timestamp
- comments belong to one document

### Tasks

- a task belongs to exactly one workspace
- assignee must be a valid workspace member
- task status transitions must pass through a workflow policy

## Non-functional constraints for Phase 1

- all APIs must be tenant-aware
- every write path must produce structured audit logs
- every request path must carry correlation and trace IDs
- every module should expose latency and error metrics

## What to avoid in Phase 1

- shared util modules with hidden domain logic
- direct repository access across modules
- generic CRUD endpoints with no module ownership semantics
- premature service extraction
- background-job dependence for core request correctness
