# Phase 1 Domain Model Outline

## Identity and Access

### Core entities

- User
- Membership
- RoleBinding
- Session

### Core invariants

- membership is scoped to one workspace
- role bindings are workspace-scoped
- suspended users cannot create authenticated sessions

## Tenant Management

### Core entities

- Tenant
- Workspace
- WorkspaceSettings

### Core invariants

- workspace belongs to exactly one tenant
- tenant status governs workspace activation
- tenant plan constrains enabled capabilities

## Messaging

### Core entities

- Channel
- ChannelMembership
- Message
- MessageThread

### Core invariants

- channel belongs to one workspace
- message belongs to one channel
- reply belongs to one parent message
- actor must be a workspace member to post

## Documents

### Core entities

- Document
- DocumentVersion
- DocumentComment

### Core invariants

- document belongs to one workspace
- version history is append-oriented
- comment belongs to one document

## Tasks

### Core entities

- Task
- TaskAssignment
- TaskComment
- TaskStatusPolicy

### Core invariants

- task belongs to one workspace
- assignee must belong to workspace
- status transitions must be validated
