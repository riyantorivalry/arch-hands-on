export type BootstrapTenantRequest = {
  tenantName: string;
  workspaceName: string;
  ownerUserId: string;
  ownerEmail: string;
  ownerDisplayName: string;
};

export type LoginRequest = {
  userId: string;
  workspaceId: string;
};

export type LoginResponse = {
  token: string;
  userId: string;
  tenantId: string;
  workspaceId: string;
  expiresAt: string;
};

export type MeResponse = {
  userId: string;
  displayName: string;
  email: string;
  workspaceId: string;
  tenantId: string;
  role: string;
};

export type Channel = {
  channelId: string;
  workspaceId: string;
  name: string;
};

export type Message = {
  messageId: string;
  channelId: string;
  authorUserId: string;
  body: string;
  parentMessageId: string | null;
};

export type DocumentItem = {
  documentId: string;
  workspaceId: string;
  title: string;
  content: string;
  status: string;
};

export type TaskItem = {
  taskId: string;
  workspaceId: string;
  title: string;
  description: string;
  status: string;
  assigneeUserId: string | null;
};

export type MembershipAssignment = {
  tenantId: string;
  workspaceId: string;
  userId: string;
  role: string;
  status: string;
};

export type SessionState = {
  token: string;
  tenantId: string;
  workspaceId: string;
  userId: string;
};

export type ApiErrorPayload = {
  code?: string;
  message?: string;
  timestamp?: string;
};
