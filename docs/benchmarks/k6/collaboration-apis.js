import http from 'k6/http';
import { check, group, sleep } from 'k6';
import exec from 'k6/execution';

const BASE_URL = (__ENV.BASE_URL || 'http://localhost:8080').replace(/\/$/, '');
const VUS = Number(__ENV.VUS || '10');
const READ_ONLY = (__ENV.READ_ONLY || 'false').toLowerCase() === 'true';
const THINK_TIME_SECONDS = Number(__ENV.THINK_TIME_SECONDS || '0.2');

export const options = {
  scenarios: {
    collaboration_apis: {
      executor: 'ramping-vus',
      stages: [
        { duration: __ENV.RAMP_UP || '30s', target: VUS },
        { duration: __ENV.HOLD || '2m', target: VUS },
        { duration: __ENV.RAMP_DOWN || '30s', target: 0 },
      ],
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: [`p(95)<${Number(__ENV.P95_MS || '1000')}`],
  },
};

export function setup() {
  const suffix = `${Date.now()}-${Math.floor(Math.random() * 100000)}`;
  const tenantName = `K6 Collaboration ${suffix}`;
  const workspaceName = `Collaboration ${suffix}`;
  const ownerUserId = `user-k6-collab-owner-${suffix}`;
  const memberUserId = `user-k6-collab-member-${suffix}`;

  const createTenant = postJson('/api/tenants', null, {
    tenantName,
    workspaceName,
    ownerUserId,
    ownerEmail: `${ownerUserId}@example.com`,
    ownerDisplayName: 'K6 Collaboration Owner',
  });
  expect(createTenant, 'create tenant', 200);

  const tenant = createTenant.json();
  const ownerToken = login(ownerUserId, tenant.workspaceId);

  const createMember = postJson(`/api/workspaces/${tenant.workspaceId}/memberships`, ownerToken, {
    userId: memberUserId,
    email: `${memberUserId}@example.com`,
    displayName: 'K6 Collaboration Member',
    role: 'MEMBER',
  });
  expect(createMember, 'create member', 200);

  const memberToken = login(memberUserId, tenant.workspaceId);

  const channel = postJson(`/api/workspaces/${tenant.workspaceId}/channels`, ownerToken, {
    name: `general-${suffix}`,
  });
  expect(channel, 'create channel', 200);

  const document = postJson(`/api/workspaces/${tenant.workspaceId}/documents`, ownerToken, {
    title: `K6 Seed Document ${suffix}`,
    content: 'Seed document for normal collaboration API read paths',
  });
  expect(document, 'create seed document', 200);

  const task = postJson(`/api/workspaces/${tenant.workspaceId}/tasks`, ownerToken, {
    title: `K6 Seed Task ${suffix}`,
    description: 'Seed task for normal collaboration API read paths',
    assigneeUserId: ownerUserId,
  });
  expect(task, 'create seed task', 200);

  return {
    tenantId: tenant.tenantId,
    workspaceId: tenant.workspaceId,
    ownerUserId,
    memberUserId,
    ownerToken,
    memberToken,
    channelId: channel.json('channelId'),
    documentId: document.json('documentId'),
    taskId: task.json('taskId'),
  };
}

export default function (ctx) {
  const actor = selectActor(ctx);

  group('identity and workspace reads', () => {
    expectOk(http.get(`${BASE_URL}/api/me`, authHeaders(actor.token)), 'get me');
    expectOk(http.get(`${BASE_URL}/api/workspaces/${ctx.workspaceId}`, authHeaders(actor.token)), 'get workspace');
    expectOk(http.get(`${BASE_URL}/api/workspaces/${ctx.workspaceId}/memberships/me`, authHeaders(actor.token)), 'get own membership');
  });

  group('messaging reads', () => {
    expectOk(http.get(`${BASE_URL}/api/workspaces/${ctx.workspaceId}/channels?page=0&size=20`, authHeaders(actor.token)), 'list channels');
    expectOk(http.get(`${BASE_URL}/api/channels/${ctx.channelId}/messages?page=0&size=20`, authHeaders(actor.token)), 'list messages');
  });

  group('document reads', () => {
    expectOk(http.get(`${BASE_URL}/api/workspaces/${ctx.workspaceId}/documents?page=0&size=20`, authHeaders(actor.token)), 'list documents');
    expectOk(http.get(`${BASE_URL}/api/documents/${ctx.documentId}`, authHeaders(actor.token)), 'get document');
    expectOk(http.get(`${BASE_URL}/api/workspaces/${ctx.workspaceId}/documents/search?query=k6&page=0&size=20`, authHeaders(actor.token)), 'search documents');
  });

  group('task reads', () => {
    expectOk(http.get(`${BASE_URL}/api/workspaces/${ctx.workspaceId}/tasks?page=0&size=20`, authHeaders(actor.token)), 'list tasks');
    expectOk(http.get(`${BASE_URL}/api/tasks/${ctx.taskId}`, authHeaders(actor.token)), 'get task');
  });

  if (!READ_ONLY) {
    writeWorkflow(ctx, actor);
  }

  sleep(THINK_TIME_SECONDS);
}

function writeWorkflow(ctx, actor) {
  const suffix = `${exec.vu.idInTest}-${exec.scenario.iterationInTest}-${Date.now()}`;

  group('messaging writes', () => {
    const message = postJson(`/api/channels/${ctx.channelId}/messages`, actor.token, {
      body: `K6 collaboration message ${suffix}`,
    });
    expectOk(message, 'post message');

    const messageId = message.json('messageId');
    if (messageId) {
      expectOk(postJson(`/api/messages/${messageId}/replies`, actor.token, {
        body: `K6 collaboration reply ${suffix}`,
      }), 'reply to message');
    }
  });

  group('document writes', () => {
    const document = postJson(`/api/workspaces/${ctx.workspaceId}/documents`, actor.token, {
      title: `K6 Document ${suffix}`,
      content: `K6 collaboration document content ${suffix}`,
    });
    expectOk(document, 'create document');

    const documentId = document.json('documentId');
    if (documentId) {
      expectOk(postJson(`/api/documents/${documentId}/comments`, actor.token, {
        body: `K6 document comment ${suffix}`,
      }), 'comment on document');

      expectOk(http.patch(`${BASE_URL}/api/documents/${documentId}`, JSON.stringify({
        title: `K6 Document ${suffix}`,
        content: `K6 collaboration document content ${suffix}`,
        status: 'IN_REVIEW',
      }), authHeaders(actor.token)), 'update document');
    }
  });

  group('task writes', () => {
    const task = postJson(`/api/workspaces/${ctx.workspaceId}/tasks`, actor.token, {
      title: `K6 Task ${suffix}`,
      description: `K6 collaboration task description ${suffix}`,
      assigneeUserId: actor.userId,
    });
    expectOk(task, 'create task');

    const taskId = task.json('taskId');
    if (taskId) {
      expectOk(postJson(`/api/tasks/${taskId}/comments`, actor.token, {
        body: `K6 task comment ${suffix}`,
      }), 'comment on task');

      expectOk(http.patch(`${BASE_URL}/api/tasks/${taskId}`, JSON.stringify({
        title: `K6 Task ${suffix}`,
        description: `K6 collaboration task description ${suffix}`,
        status: 'IN_PROGRESS',
        assigneeUserId: actor.userId,
      }), authHeaders(actor.token)), 'update task');
    }
  });
}

function selectActor(ctx) {
  if (exec.vu.idInTest % 2 === 0) {
    return {
      userId: ctx.memberUserId,
      token: ctx.memberToken,
    };
  }
  return {
    userId: ctx.ownerUserId,
    token: ctx.ownerToken,
  };
}

function login(userId, workspaceId) {
  const res = postJson('/api/auth/login', null, { userId, workspaceId });
  expect(res, `login ${userId}`, 200);
  return res.json('token');
}

function postJson(path, token, body) {
  return http.post(`${BASE_URL}${path}`, JSON.stringify(body), authHeaders(token));
}

function authHeaders(token) {
  const headers = {
    'Content-Type': 'application/json',
    Accept: 'application/json',
  };
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }
  return { headers };
}

function expectOk(res, label) {
  check(res, {
    [`${label} returned 2xx`]: (r) => r.status >= 200 && r.status < 300,
  });
}

function expect(res, label, status) {
  if (res.status !== status) {
    throw new Error(`${label} expected ${status}, got ${res.status}: ${res.body}`);
  }
}
