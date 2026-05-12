# Bounded Contexts

## Phase 1 candidate bounded contexts

- Identity and Access
- Tenant Management
- Messaging
- Documents
- Tasks
- Notifications
- Search
- Files
- Analytics
- AI Assistant
- Operations / Admin

## Recommended Phase 1 implementation scope

Keep the initial build narrower:

- Identity and Access
- Tenant Management
- Messaging
- Documents
- Tasks

## Phase 1 implementation objective

Phase 1 should produce a modular monolith that can support the first credible collaboration workflows without pulling in all later-stage platform concerns.

The first release cut should support:

- tenant creation and workspace bootstrap
- user login and membership resolution
- channel creation
- chat messages and threads
- documents and comments
- tasks with assignment and status transitions

## Phase 1 context map

### Core domains

- Identity and Access
- Tenant Management
- Messaging
- Documents
- Tasks

### Supporting domains deferred to later phases

- Notifications
- Search
- Files
- Analytics
- AI Assistant
- Operations / Admin

### Dependency direction

- Identity and Access -> no dependency on business modules
- Tenant Management -> depends only on Identity and Access contracts where needed
- Messaging -> depends on Identity and Access and Tenant Management contracts
- Documents -> depends on Identity and Access and Tenant Management contracts
- Tasks -> depends on Identity and Access and Tenant Management contracts

Peer business modules should not call each other's repositories or internal domain objects directly.

## Detailed bounded contexts

### 1. Identity and Access

**Purpose:** Authenticate users, manage identities, memberships, and role resolution.

**Owns:**

- user identity
- credential / external identity linkage
- session or token metadata
- workspace membership
- role assignment

**Top-level aggregates / entities:**

- User
- IdentityProviderLink
- Session
- Membership
- RoleBinding

**Primary responsibilities:**

- user registration or first-login provisioning
- login / logout flows
- workspace membership lookup
- role resolution for authorization
- actor identity for auditing

**Does not own:**

- tenant lifecycle
- channel permissions beyond exposing membership and roles
- document or task authorization rules beyond identity facts

**Inbound use cases:**

- authenticate actor
- resolve current user profile
- check workspace membership
- assign or revoke workspace role

**Outbound contracts exposed to other modules:**

- `CurrentActorResolver`
- `MembershipReader`
- `RoleReader`

### 2. Tenant Management

**Purpose:** Manage tenant and workspace lifecycle, settings, and isolation metadata.

**Owns:**

- tenant
- workspace
- workspace settings
- plan / tier metadata
- tenant status

**Top-level aggregates / entities:**

- Tenant
- Workspace
- WorkspaceSettings
- TenantPlan

**Primary responsibilities:**

- create tenant and initial workspace
- manage tenant/workspace status
- expose tenant-scoped configuration
- maintain canonical tenant and workspace identifiers

**Does not own:**

- user membership rules
- channel membership
- document permissions
- task assignment logic

**Inbound use cases:**

- create tenant
- create workspace
- update workspace settings
- suspend or reactivate tenant

**Outbound contracts exposed to other modules:**

- `TenantReader`
- `WorkspaceReader`
- `TenantPolicyReader`

### 3. Messaging

**Purpose:** Handle chat channels, messages, threads, and message visibility rules.

**Owns:**

- channel
- message
- thread
- message reactions if included in Phase 1

**Top-level aggregates / entities:**

- Channel
- ChannelMembership
- Message
- MessageThread

**Primary responsibilities:**

- create and manage channels
- post messages
- reply in threads
- fetch conversation history
- emit domain events for downstream notifications and indexing later

**Does not own:**

- global user identity
- tenant lifecycle
- document content
- task workflows

**Inbound use cases:**

- create channel
- join or leave channel
- post message
- reply to thread
- list messages

**Required upstream facts:**

- current actor identity
- workspace membership
- tenant/workspace validity

**Outbound contracts exposed to other modules:**

- `ChannelReader`
- `MessageReader`

### 4. Documents

**Purpose:** Manage collaborative documents, structured content metadata, and discussion comments.

**Owns:**

- document
- document comment
- document version metadata
- document sharing state within a workspace

**Top-level aggregates / entities:**

- Document
- DocumentComment
- DocumentVersion

**Primary responsibilities:**

- create documents
- edit document metadata and content
- manage version snapshots or version references
- attach comments to documents

**Does not own:**

- file blob storage pipeline
- search indexing
- AI retrieval
- task state transitions

**Inbound use cases:**

- create document
- update document
- comment on document
- list workspace documents
- fetch version history

**Required upstream facts:**

- actor identity
- workspace membership
- tenant/workspace validity

**Outbound contracts exposed to other modules:**

- `DocumentReader`

### 5. Tasks

**Purpose:** Manage work items, assignment, state transitions, and task comments.

**Owns:**

- task
- task status workflow
- task assignment
- task comments
- due date and priority metadata

**Top-level aggregates / entities:**

- Task
- TaskAssignment
- TaskComment
- TaskStatusPolicy

**Primary responsibilities:**

- create tasks
- assign users
- transition task state
- comment on tasks
- list tasks by workspace, assignee, or status

**Does not own:**

- user identity source
- tenant lifecycle
- document editing
- message history

**Inbound use cases:**

- create task
- assign task
- change status
- add comment
- query task board / backlog

**Required upstream facts:**

- actor identity
- workspace membership
- user existence and assignee validity

**Outbound contracts exposed to other modules:**

- `TaskReader`

## Context boundary rules

- business logic stays within owning module
- cross-context access goes through explicit interfaces
- shared database access does not imply shared ownership
- asynchronous integration is preferred for derived views and notifications

## Cross-context integration rules

- Identity and Access is the source of truth for `user`, `membership`, and `role` facts.
- Tenant Management is the source of truth for `tenant` and `workspace` facts.
- Messaging, Documents, and Tasks may store foreign keys to users and workspaces, but not duplicate upstream ownership logic.
- Cross-context reads inside the monolith should use application-service or contract interfaces, not direct table joins across module ownership boundaries in domain code.
- Shared reporting queries are allowed only through explicit read-model paths.

## Shared concepts allowed across modules

The following identifiers may be shared as references:

- `tenant_id`
- `workspace_id`
- `user_id`

The following should not be shared as mutable domain objects:

- `User`
- `Workspace`
- `Task`
- `Document`
- `Message`

## Suggested Java module layout

Each Phase 1 bounded context should follow a similar package structure:

```text
com.example.platform.<module>
  api
  application
  domain
  infrastructure
```

### Package intent

- `api` - controllers, DTOs, request/response models
- `application` - use cases, orchestration, transactional boundaries
- `domain` - aggregates, policies, domain services, repository ports
- `infrastructure` - JPA repositories, adapters, messaging, persistence mapping

## Recommended first implementation slice

Build the first end-to-end slice in this order:

1. Identity and Access
2. Tenant Management
3. Messaging
4. Documents
5. Tasks

That order keeps dependency direction clean and gives a usable collaboration baseline early.

## Next step

Define concrete module contracts, API surface, and initial aggregate rules for the first Java implementation.
