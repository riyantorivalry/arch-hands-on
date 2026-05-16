import type {
  ApiErrorPayload,
  BootstrapTenantRequest,
  Channel,
  DocumentItem,
  LoginRequest,
  LoginResponse,
  MembershipAssignment,
  MeResponse,
  Message,
  SessionState,
  TaskItem
} from "./types";

const API_BASE = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080/api";

type RequestOptions = {
  method?: "GET" | "POST" | "PATCH";
  token?: string;
  body?: unknown;
};

export class ApiError extends Error {
  readonly status: number;
  readonly code?: string;

  constructor(status: number, message: string, code?: string) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.code = code;
  }
}

async function apiRequest<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const headers: Record<string, string> = {
    "Content-Type": "application/json"
  };

  if (options.token) {
    headers.Authorization = `Bearer ${options.token}`;
  }

  const response = await fetch(`${API_BASE}${path}`, {
    method: options.method ?? "GET",
    headers,
    body: options.body ? JSON.stringify(options.body) : undefined,
    cache: "no-store"
  });

  if (!response.ok) {
    const raw = await response.text();
    const parsed = tryParseApiError(raw);
    throw new ApiError(
      response.status,
      parsed?.message ?? raw ?? `Request failed: ${response.status}`,
      parsed?.code
    );
  }

  return (await response.json()) as T;
}

export async function bootstrapTenant(payload: BootstrapTenantRequest) {
  return apiRequest<{ tenantId: string; workspaceId: string; status: string }>("/tenants", {
    method: "POST",
    body: payload
  });
}

export async function login(payload: LoginRequest) {
  return apiRequest<LoginResponse>("/auth/login", {
    method: "POST",
    body: payload
  });
}

export async function logout(token: string) {
  return apiRequest<{ status: string }>("/auth/logout", {
    method: "POST",
    token
  });
}

export async function fetchCurrentUser(token: string) {
  return apiRequest<MeResponse>("/me", { token });
}

export async function fetchChannels(session: SessionState) {
  return apiRequest<Channel[]>(`/workspaces/${session.workspaceId}/channels`, { token: session.token });
}

export async function createChannel(session: SessionState, name: string) {
  return apiRequest<Channel>(`/workspaces/${session.workspaceId}/channels`, {
    method: "POST",
    token: session.token,
    body: { name }
  });
}

export async function fetchMessages(session: SessionState, channelId: string) {
  return apiRequest<Message[]>(`/channels/${channelId}/messages`, { token: session.token });
}

export async function postMessage(session: SessionState, channelId: string, body: string) {
  return apiRequest<Message>(`/channels/${channelId}/messages`, {
    method: "POST",
    token: session.token,
    body: { body }
  });
}

export async function fetchDocuments(session: SessionState) {
  return apiRequest<DocumentItem[]>(`/workspaces/${session.workspaceId}/documents`, { token: session.token });
}

export async function createDocument(session: SessionState, title: string, content: string) {
  return apiRequest<DocumentItem>(`/workspaces/${session.workspaceId}/documents`, {
    method: "POST",
    token: session.token,
    body: { title, content }
  });
}

export async function updateDocument(session: SessionState, documentId: string, title: string, content: string, status: string) {
  return apiRequest<DocumentItem>(`/documents/${documentId}`, {
    method: "PATCH",
    token: session.token,
    body: { title, content, status }
  });
}

export async function fetchTasks(session: SessionState) {
  return apiRequest<TaskItem[]>(`/workspaces/${session.workspaceId}/tasks`, { token: session.token });
}

export async function createTask(
  session: SessionState,
  title: string,
  description: string,
  assigneeUserId: string
) {
  return apiRequest<TaskItem>(`/workspaces/${session.workspaceId}/tasks`, {
    method: "POST",
    token: session.token,
    body: { title, description, assigneeUserId: assigneeUserId || null }
  });
}

export async function updateTask(
  session: SessionState,
  taskId: string,
  title: string,
  description: string,
  status: string,
  assigneeUserId: string
) {
  return apiRequest<TaskItem>(`/tasks/${taskId}`, {
    method: "PATCH",
    token: session.token,
    body: {
      title,
      description,
      status,
      assigneeUserId: assigneeUserId || null
    }
  });
}

export async function assignMembership(
  session: SessionState,
  userId: string,
  email: string,
  displayName: string,
  role: string
) {
  return apiRequest<MembershipAssignment>(`/workspaces/${session.workspaceId}/memberships`, {
    method: "POST",
    token: session.token,
    body: { userId, email, displayName, role }
  });
}

function tryParseApiError(raw: string): ApiErrorPayload | null {
  if (!raw) {
    return null;
  }
  try {
    return JSON.parse(raw) as ApiErrorPayload;
  } catch {
    return null;
  }
}
