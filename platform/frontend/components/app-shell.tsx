"use client";

import { useEffect, useMemo, useState } from "react";
import {
  assignMembership,
  bootstrapTenant,
  createChannel,
  createDocument,
  createTask,
  fetchChannels,
  fetchCurrentUser,
  fetchDocuments,
  fetchMessages,
  fetchTasks,
  login,
  logout,
  postMessage,
  updateDocument,
  updateTask
} from "../lib/api";
import { clearSession, readSession, writeSession } from "../lib/session";
import type { Channel, DocumentItem, MeResponse, MembershipAssignment, Message, SessionState, TaskItem } from "../lib/types";

type BootstrapForm = {
  tenantName: string;
  workspaceName: string;
  ownerUserId: string;
  ownerEmail: string;
  ownerDisplayName: string;
};

type LoginForm = {
  userId: string;
  workspaceId: string;
};

const bootstrapDefaults: BootstrapForm = {
  tenantName: "Acme Corp",
  workspaceName: "Engineering",
  ownerUserId: "user-alice",
  ownerEmail: "alice@example.com",
  ownerDisplayName: "Alice"
};

const loginDefaults: LoginForm = {
  userId: "user-alice",
  workspaceId: "workspace-engineering"
};

export function AppShell() {
  const [session, setSession] = useState<SessionState | null>(null);
  const [currentUser, setCurrentUser] = useState<MeResponse | null>(null);
  const [channels, setChannels] = useState<Channel[]>([]);
  const [documents, setDocuments] = useState<DocumentItem[]>([]);
  const [tasks, setTasks] = useState<TaskItem[]>([]);
  const [messages, setMessages] = useState<Message[]>([]);
  const [selectedChannelId, setSelectedChannelId] = useState("");
  const [selectedDocumentId, setSelectedDocumentId] = useState("");
  const [selectedTaskId, setSelectedTaskId] = useState("");
  const [error, setError] = useState("");
  const [status, setStatus] = useState("Idle");

  const [bootstrapForm, setBootstrapForm] = useState<BootstrapForm>(bootstrapDefaults);
  const [loginForm, setLoginForm] = useState<LoginForm>(loginDefaults);
  const [memberForm, setMemberForm] = useState({
    userId: "user-bob",
    email: "bob@example.com",
    displayName: "Bob",
    role: "MEMBER"
  });
  const [channelName, setChannelName] = useState("general");
  const [messageBody, setMessageBody] = useState("Hello team");
  const [documentForm, setDocumentForm] = useState({
    title: "Architecture Notes",
    content: "Initial collaboration platform notes"
  });
  const [documentEditForm, setDocumentEditForm] = useState({
    title: "",
    content: ""
  });
  const [taskForm, setTaskForm] = useState({
    title: "Bootstrap API",
    description: "Implement the first API slice",
    assigneeUserId: "user-alice"
  });
  const [taskEditForm, setTaskEditForm] = useState({
    title: "",
    description: "",
    status: "TODO",
    assigneeUserId: ""
  });

  useEffect(() => {
    const existing = readSession();
    if (existing) {
      setSession(existing);
    }
  }, []);

  useEffect(() => {
    if (!session) {
      setCurrentUser(null);
      setChannels([]);
      setDocuments([]);
      setTasks([]);
      setMessages([]);
      setSelectedChannelId("");
      setSelectedDocumentId("");
      setSelectedTaskId("");
      return;
    }

    void refreshWorkspace(session);
  }, [session]);

  useEffect(() => {
    if (!session || !selectedChannelId) {
      setMessages([]);
      return;
    }
    void loadMessages(session, selectedChannelId);
  }, [session, selectedChannelId]);

  useEffect(() => {
    const selectedDocument = documents.find((item) => item.documentId === selectedDocumentId);
    if (selectedDocument) {
      setDocumentEditForm({
        title: selectedDocument.title,
        content: selectedDocument.content
      });
    }
  }, [documents, selectedDocumentId]);

  useEffect(() => {
    const selectedTask = tasks.find((item) => item.taskId === selectedTaskId);
    if (selectedTask) {
      setTaskEditForm({
        title: selectedTask.title,
        description: selectedTask.description,
        status: selectedTask.status,
        assigneeUserId: selectedTask.assigneeUserId ?? ""
      });
    }
  }, [tasks, selectedTaskId]);

  const canManageWorkspace = useMemo(() => {
    return currentUser?.role === "OWNER" || currentUser?.role === "ADMIN";
  }, [currentUser]);

  const canUpdateResources = canManageWorkspace;

  const selectedDocument = useMemo(
    () => documents.find((item) => item.documentId === selectedDocumentId) ?? null,
    [documents, selectedDocumentId]
  );

  const selectedTask = useMemo(
    () => tasks.find((item) => item.taskId === selectedTaskId) ?? null,
    [tasks, selectedTaskId]
  );

  async function refreshWorkspace(activeSession: SessionState) {
    try {
      setError("");
      setStatus("Loading workspace");
      const [user, nextChannels, nextDocuments, nextTasks] = await Promise.all([
        fetchCurrentUser(activeSession.token),
        fetchChannels(activeSession),
        fetchDocuments(activeSession),
        fetchTasks(activeSession)
      ]);
      setCurrentUser(user);
      setChannels(nextChannels);
      setDocuments(nextDocuments);
      setTasks(nextTasks);
      setSelectedChannelId((current) => current || nextChannels[0]?.channelId || "");
      setSelectedDocumentId((current) => current || nextDocuments[0]?.documentId || "");
      setSelectedTaskId((current) => current || nextTasks[0]?.taskId || "");
      setStatus("Workspace ready");
    } catch (cause) {
      clearSession();
      setSession(null);
      setError(readError(cause));
      setStatus("Session reset");
    }
  }

  async function loadMessages(activeSession: SessionState, channelId: string) {
    try {
      const nextMessages = await fetchMessages(activeSession, channelId);
      setMessages(nextMessages);
    } catch (cause) {
      setError(readError(cause));
    }
  }

  async function handleBootstrap(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    try {
      setError("");
      setStatus("Creating tenant");
      const result = await bootstrapTenant(bootstrapForm);
      setLoginForm({
        userId: bootstrapForm.ownerUserId,
        workspaceId: result.workspaceId
      });
      setStatus(`Tenant created: ${result.tenantId}`);
    } catch (cause) {
      setError(readError(cause));
    }
  }

  async function handleLogin(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    try {
      setError("");
      setStatus("Signing in");
      const result = await login(loginForm);
      const nextSession: SessionState = {
        token: result.token,
        tenantId: result.tenantId,
        workspaceId: result.workspaceId,
        userId: result.userId
      };
      writeSession(nextSession);
      setSession(nextSession);
      setStatus("Signed in");
    } catch (cause) {
      setError(readError(cause));
    }
  }

  async function handleLogout() {
    if (!session) {
      return;
    }
    try {
      await logout(session.token);
    } catch {
      // Intentionally ignore remote logout failures.
    }
    clearSession();
    setSession(null);
    setStatus("Signed out");
  }

  async function handleCreateChannel(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!session || !canManageWorkspace) {
      return;
    }
    try {
      setError("");
      const created = await createChannel(session, channelName);
      setChannels((current) => [...current, created]);
      setSelectedChannelId(created.channelId);
      setChannelName("");
      setStatus(`Channel created: ${created.name}`);
    } catch (cause) {
      setError(readError(cause));
    }
  }

  async function handlePostMessage(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!session || !selectedChannelId) {
      return;
    }
    try {
      setError("");
      await postMessage(session, selectedChannelId, messageBody);
      setMessageBody("");
      await loadMessages(session, selectedChannelId);
      setStatus("Message posted");
    } catch (cause) {
      setError(readError(cause));
    }
  }

  async function handleCreateDocument(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!session) {
      return;
    }
    try {
      setError("");
      const created = await createDocument(session, documentForm.title, documentForm.content);
      setDocuments((current) => [created, ...current]);
      setSelectedDocumentId(created.documentId);
      setDocumentForm({ title: "", content: "" });
      setStatus(`Document created: ${created.title}`);
    } catch (cause) {
      setError(readError(cause));
    }
  }

  async function handleUpdateDocument(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!session || !selectedDocument || !canUpdateResources) {
      return;
    }
    try {
      setError("");
      const updated = await updateDocument(session, selectedDocument.documentId, documentEditForm.title, documentEditForm.content);
      setDocuments((current) => current.map((item) => (item.documentId === updated.documentId ? updated : item)));
      setStatus(`Document updated: ${updated.title}`);
    } catch (cause) {
      setError(readError(cause));
    }
  }

  async function handleCreateTask(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!session) {
      return;
    }
    try {
      setError("");
      const created = await createTask(session, taskForm.title, taskForm.description, taskForm.assigneeUserId);
      setTasks((current) => [created, ...current]);
      setSelectedTaskId(created.taskId);
      setTaskForm({ title: "", description: "", assigneeUserId: currentUser?.userId ?? "" });
      setStatus(`Task created: ${created.title}`);
    } catch (cause) {
      setError(readError(cause));
    }
  }

  async function handleUpdateTask(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!session || !selectedTask || !canUpdateResources) {
      return;
    }
    try {
      setError("");
      const updated = await updateTask(
        session,
        selectedTask.taskId,
        taskEditForm.title,
        taskEditForm.description,
        taskEditForm.status,
        taskEditForm.assigneeUserId
      );
      setTasks((current) => current.map((item) => (item.taskId === updated.taskId ? updated : item)));
      setStatus(`Task updated: ${updated.title}`);
    } catch (cause) {
      setError(readError(cause));
    }
  }

  async function handleAssignMembership(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!session || !canManageWorkspace) {
      return;
    }
    try {
      setError("");
      const result: MembershipAssignment = await assignMembership(
        session,
        memberForm.userId,
        memberForm.email,
        memberForm.displayName,
        memberForm.role
      );
      setTaskForm((existing) => ({ ...existing, assigneeUserId: result.userId }));
      setStatus(`Member assigned: ${result.userId} (${result.role})`);
    } catch (cause) {
      setError(readError(cause));
    }
  }

  return (
    <main className="page">
      <div className="shell">
        <aside className="sidebar">
          <h1 className="brand">Collaboration Platform</h1>
          <p className="muted">Phase 1 frontend targeting the current Java backend and session-based auth.</p>

          <div className="stack">
            <section className="panel">
              <h2>Bootstrap Workspace</h2>
              <form className="form" onSubmit={handleBootstrap}>
                <Field label="Tenant Name" value={bootstrapForm.tenantName} onChange={(value) => setBootstrapForm({ ...bootstrapForm, tenantName: value })} />
                <Field label="Workspace Name" value={bootstrapForm.workspaceName} onChange={(value) => setBootstrapForm({ ...bootstrapForm, workspaceName: value })} />
                <Field label="Owner User ID" value={bootstrapForm.ownerUserId} onChange={(value) => setBootstrapForm({ ...bootstrapForm, ownerUserId: value })} />
                <Field label="Owner Email" value={bootstrapForm.ownerEmail} onChange={(value) => setBootstrapForm({ ...bootstrapForm, ownerEmail: value })} />
                <Field label="Owner Display Name" value={bootstrapForm.ownerDisplayName} onChange={(value) => setBootstrapForm({ ...bootstrapForm, ownerDisplayName: value })} />
                <div className="actions">
                  <button className="button button-primary" type="submit">Create Tenant</button>
                </div>
              </form>
            </section>

            <section className="panel">
              <h2>Login</h2>
              <form className="form" onSubmit={handleLogin}>
                <Field label="User ID" value={loginForm.userId} onChange={(value) => setLoginForm({ ...loginForm, userId: value })} />
                <Field label="Workspace ID" value={loginForm.workspaceId} onChange={(value) => setLoginForm({ ...loginForm, workspaceId: value })} />
                <div className="actions">
                  <button className="button button-primary" type="submit">Sign In</button>
                </div>
              </form>
            </section>

            <section className="panel">
              <h2>Session</h2>
              <div className="list">
                <div className="item">
                  <strong>Status</strong>
                  <span className="meta">{status}</span>
                </div>
                <div className="item">
                  <strong>User</strong>
                  <span className="meta">{currentUser?.displayName ?? "Not signed in"}</span>
                </div>
                <div className="item">
                  <strong>Workspace</strong>
                  <span className="meta">{currentUser?.workspaceId ?? "-"}</span>
                </div>
                <div className="actions">
                  <button className="button button-danger" type="button" onClick={handleLogout}>
                    Sign Out
                  </button>
                </div>
              </div>
            </section>

            {canManageWorkspace ? (
              <section className="panel">
                <h2>Workspace Access</h2>
                <form className="form" onSubmit={handleAssignMembership}>
                  <Field label="User ID" value={memberForm.userId} onChange={(value) => setMemberForm({ ...memberForm, userId: value })} />
                  <Field label="Email" value={memberForm.email} onChange={(value) => setMemberForm({ ...memberForm, email: value })} />
                  <Field label="Display Name" value={memberForm.displayName} onChange={(value) => setMemberForm({ ...memberForm, displayName: value })} />
                  <SelectField
                    label="Role"
                    value={memberForm.role}
                    options={["MEMBER", "ADMIN"]}
                    onChange={(value) => setMemberForm({ ...memberForm, role: value })}
                  />
                  <div className="actions">
                    <button className="button" type="submit">Assign Member</button>
                  </div>
                </form>
              </section>
            ) : null}

            {error ? <p className="error">{error}</p> : null}
          </div>
        </aside>

        <section className="content">
          <div className="topbar">
            <div>
              <h2 className="title">Workspace Dashboard</h2>
              <p className="muted">
                {currentUser
                  ? `${currentUser.displayName} | ${currentUser.role} | ${currentUser.workspaceId}`
                  : "Authenticate to load workspace data."}
              </p>
            </div>
            {currentUser ? <span className="status">{currentUser.role}</span> : null}
          </div>

          <div className="grid">
            <section className="panel">
              <div className="section-header">
                <h3>Channels</h3>
                <button className="button" type="button" onClick={() => session && void refreshWorkspace(session)}>
                  Refresh
                </button>
              </div>
              <form className="form" onSubmit={handleCreateChannel}>
                <Field label="Channel Name" value={channelName} onChange={setChannelName} />
                <div className="actions">
                  <button className="button button-primary" disabled={!canManageWorkspace} type="submit">Create Channel</button>
                </div>
                {!canManageWorkspace ? <p className="notice">Channel creation requires an owner or admin role.</p> : null}
              </form>
              <div className="list">
                {channels.length === 0 ? (
                  <div className="empty">No channels loaded.</div>
                ) : (
                  channels.map((channel) => (
                    <button
                      key={channel.channelId}
                      className="item"
                      type="button"
                      onClick={() => setSelectedChannelId(channel.channelId)}
                    >
                      <strong>{channel.name}</strong>
                      <span className="meta">{channel.channelId}</span>
                    </button>
                  ))
                )}
              </div>

              <div className="section-header" style={{ marginTop: 16 }}>
                <h3>Messages</h3>
                <span className="meta">{selectedChannelId || "No channel selected"}</span>
              </div>
              <form className="form" onSubmit={handlePostMessage}>
                <Field label="Message" value={messageBody} onChange={setMessageBody} />
                <div className="actions">
                  <button className="button" type="submit">Post Message</button>
                </div>
              </form>
              <div className="list">
                {messages.length === 0 ? (
                  <div className="empty">No messages loaded.</div>
                ) : (
                  messages.map((message) => (
                    <div className="item" key={message.messageId}>
                      <strong>{message.authorUserId}</strong>
                      <div>{message.body}</div>
                    </div>
                  ))
                )}
              </div>
            </section>

            <section className="panel">
              <div className="section-header">
                <h3>Documents</h3>
                <button className="button" type="button" onClick={() => session && void refreshWorkspace(session)}>
                  Refresh
                </button>
              </div>
              <form className="form" onSubmit={handleCreateDocument}>
                <Field label="Title" value={documentForm.title} onChange={(value) => setDocumentForm({ ...documentForm, title: value })} />
                <TextAreaField
                  label="Content"
                  value={documentForm.content}
                  onChange={(value) => setDocumentForm({ ...documentForm, content: value })}
                />
                <div className="actions">
                  <button className="button button-primary" type="submit">Create Document</button>
                </div>
              </form>
              <div className="list">
                {documents.length === 0 ? (
                  <div className="empty">No documents loaded.</div>
                ) : (
                  documents.map((document) => (
                    <button
                      className="item"
                      key={document.documentId}
                      type="button"
                      onClick={() => setSelectedDocumentId(document.documentId)}
                    >
                      <strong>{document.title}</strong>
                      <div>{document.content}</div>
                      <span className="meta">{document.status}</span>
                    </button>
                  ))
                )}
              </div>
            </section>

            <section className="panel">
              <div className="section-header">
                <h3>Tasks</h3>
                <button className="button" type="button" onClick={() => session && void refreshWorkspace(session)}>
                  Refresh
                </button>
              </div>
              <form className="form" onSubmit={handleCreateTask}>
                <Field label="Title" value={taskForm.title} onChange={(value) => setTaskForm({ ...taskForm, title: value })} />
                <TextAreaField
                  label="Description"
                  value={taskForm.description}
                  onChange={(value) => setTaskForm({ ...taskForm, description: value })}
                />
                <Field
                  label="Assignee User ID"
                  value={taskForm.assigneeUserId}
                  onChange={(value) => setTaskForm({ ...taskForm, assigneeUserId: value })}
                />
                <div className="actions">
                  <button className="button button-primary" type="submit">Create Task</button>
                </div>
              </form>
              <div className="list">
                {tasks.length === 0 ? (
                  <div className="empty">No tasks loaded.</div>
                ) : (
                  tasks.map((task) => (
                    <button
                      className="item"
                      key={task.taskId}
                      type="button"
                      onClick={() => setSelectedTaskId(task.taskId)}
                    >
                      <strong>{task.title}</strong>
                      <div>{task.description}</div>
                      <span className="meta">
                        {task.status} | {task.assigneeUserId ?? "Unassigned"}
                      </span>
                    </button>
                  ))
                )}
              </div>
            </section>
          </div>

          <div className="detail-grid">
            <section className="panel">
              <div className="section-header">
                <h3>Document Detail</h3>
                <span className="meta">{selectedDocument?.documentId ?? "No document selected"}</span>
              </div>
              {selectedDocument ? (
                <form className="form" onSubmit={handleUpdateDocument}>
                  <Field
                    label="Title"
                    value={documentEditForm.title}
                    onChange={(value) => setDocumentEditForm({ ...documentEditForm, title: value })}
                  />
                  <TextAreaField
                    label="Content"
                    value={documentEditForm.content}
                    onChange={(value) => setDocumentEditForm({ ...documentEditForm, content: value })}
                  />
                  <div className="actions">
                    <button className="button button-primary" disabled={!canUpdateResources} type="submit">
                      Update Document
                    </button>
                  </div>
                  {!canUpdateResources ? (
                    <p className="notice">
                      Update actions are currently shown only for owner/admin roles because the backend does not yet expose resource-owner metadata.
                    </p>
                  ) : null}
                </form>
              ) : (
                <div className="empty">Select a document to inspect or update it.</div>
              )}
            </section>

            <section className="panel">
              <div className="section-header">
                <h3>Task Detail</h3>
                <span className="meta">{selectedTask?.taskId ?? "No task selected"}</span>
              </div>
              {selectedTask ? (
                <form className="form" onSubmit={handleUpdateTask}>
                  <Field
                    label="Title"
                    value={taskEditForm.title}
                    onChange={(value) => setTaskEditForm({ ...taskEditForm, title: value })}
                  />
                  <TextAreaField
                    label="Description"
                    value={taskEditForm.description}
                    onChange={(value) => setTaskEditForm({ ...taskEditForm, description: value })}
                  />
                  <SelectField
                    label="Status"
                    value={taskEditForm.status}
                    options={["TODO", "IN_PROGRESS", "DONE"]}
                    onChange={(value) => setTaskEditForm({ ...taskEditForm, status: value })}
                  />
                  <Field
                    label="Assignee User ID"
                    value={taskEditForm.assigneeUserId}
                    onChange={(value) => setTaskEditForm({ ...taskEditForm, assigneeUserId: value })}
                  />
                  <div className="actions">
                    <button className="button button-primary" disabled={!canUpdateResources} type="submit">
                      Update Task
                    </button>
                  </div>
                  {!canUpdateResources ? (
                    <p className="notice">
                      Task updates are hidden for member roles in this UI until the backend returns per-task ownership metadata for finer client-side authorization.
                    </p>
                  ) : null}
                </form>
              ) : (
                <div className="empty">Select a task to inspect or update it.</div>
              )}
            </section>
          </div>
        </section>
      </div>
    </main>
  );
}

function Field({
  label,
  value,
  onChange
}: {
  label: string;
  value: string;
  onChange: (value: string) => void;
}) {
  return (
    <div className="field">
      <label>{label}</label>
      <input value={value} onChange={(event) => onChange(event.target.value)} />
    </div>
  );
}

function TextAreaField({
  label,
  value,
  onChange
}: {
  label: string;
  value: string;
  onChange: (value: string) => void;
}) {
  return (
    <div className="field">
      <label>{label}</label>
      <textarea value={value} onChange={(event) => onChange(event.target.value)} />
    </div>
  );
}

function SelectField({
  label,
  value,
  options,
  onChange
}: {
  label: string;
  value: string;
  options: string[];
  onChange: (value: string) => void;
}) {
  return (
    <div className="field">
      <label>{label}</label>
      <select value={value} onChange={(event) => onChange(event.target.value)}>
        {options.map((option) => (
          <option key={option} value={option}>
            {option}
          </option>
        ))}
      </select>
    </div>
  );
}

function readError(cause: unknown) {
  if (cause instanceof Error) {
    return cause.message;
  }
  return "Unexpected error";
}
