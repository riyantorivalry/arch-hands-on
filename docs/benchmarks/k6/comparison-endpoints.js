import http from 'k6/http';
import { check, group, sleep } from 'k6';
import exec from 'k6/execution';

const BASE_URL = (__ENV.BASE_URL || 'http://localhost:8080').replace(/\/$/, '');
const SUITE = __ENV.SUITE || 'all';
const CACHE_STRATEGIES = listEnv('CACHE_STRATEGIES', 'caffeine');
const RATE_LIMIT_ALGORITHMS = listEnv('RATE_LIMIT_ALGORITHMS', 'fixed-window,sliding-window,token-bucket');
const AUTH_ENGINES = listEnv('AUTH_ENGINES', 'in-code,opa-local,casbin-local,db-policy');
const ANALYTICS_VERSIONS = listEnv('ANALYTICS_VERSIONS', 'v1');
const REALTIME_VERSIONS = listEnv('REALTIME_VERSIONS', 'v1');
const SEARCH_VERSIONS = listEnv('SEARCH_VERSIONS', 'v1,v2,v3');
const THINK_TIME_SECONDS = Number(__ENV.THINK_TIME_SECONDS || '0.1');

export const options = {
  scenarios: {
    comparison_endpoints: {
      executor: 'ramping-vus',
      stages: [
        { duration: __ENV.RAMP_UP || '30s', target: Number(__ENV.VUS || '10') },
        { duration: __ENV.HOLD || '1m', target: Number(__ENV.VUS || '10') },
        { duration: __ENV.RAMP_DOWN || '15s', target: 0 },
      ],
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: [`p(95)<${Number(__ENV.P95_MS || '750')}`],
  },
};

export function setup() {
  const suffix = `${Date.now()}-${Math.floor(Math.random() * 100000)}`;
  const tenantName = `K6 Tenant ${suffix}`;
  const workspaceName = `K6 Workspace ${suffix}`;
  const ownerUserId = `user-k6-owner-${suffix}`;
  const memberUserId = `user-k6-member-${suffix}`;

  const createTenant = postJson('/api/tenants', null, {
    tenantName,
    workspaceName,
    ownerUserId,
    ownerEmail: `${ownerUserId}@example.com`,
    ownerDisplayName: 'K6 Owner',
  });
  expect(createTenant, 'create tenant', 200);

  const tenant = createTenant.json();
  const ownerToken = login(ownerUserId, tenant.workspaceId);

  const createMember = postJson(`/api/workspaces/${tenant.workspaceId}/memberships`, ownerToken, {
    userId: memberUserId,
    email: `${memberUserId}@example.com`,
    displayName: 'K6 Member',
    role: 'MEMBER',
  });
  expect(createMember, 'create member', 200);

  const memberToken = login(memberUserId, tenant.workspaceId);

  const document = postJson(`/api/workspaces/${tenant.workspaceId}/documents`, ownerToken, {
    title: `K6 Architecture Notes ${suffix}`,
    content: 'K6 benchmark document for search analytics realtime comparison',
  });
  expect(document, 'create document', 200);

  const search = http.get(`${BASE_URL}/api/v1/workspaces/${tenant.workspaceId}/documents/search?query=k6`, authHeaders(ownerToken));
  expect(search, 'seed search event', 200);

  return {
    tenantId: tenant.tenantId,
    workspaceId: tenant.workspaceId,
    ownerUserId,
    memberUserId,
    ownerToken,
    memberToken,
  };
}

export default function (ctx) {
  if (includesSuite('auth')) {
    authorizationModels(ctx);
    authorizationEngines(ctx);
  }
  if (includesSuite('cache')) {
    cacheStrategies(ctx);
  }
  if (includesSuite('rate-limit')) {
    rateLimitAlgorithms(ctx);
  }
  if (includesSuite('realtime')) {
    realtimePolling(ctx);
  }
  if (includesSuite('analytics')) {
    analyticsStorage(ctx);
  }
  if (includesSuite('search')) {
    documentSearch(ctx);
  }
  sleep(THINK_TIME_SECONDS);
}

function authorizationModels(ctx) {
  const body = authorizationRequest(ctx);

  group('authorization model: rbac v1', () => {
    const res = postJson(`/api/v1/workspaces/${ctx.workspaceId}/authorization/decisions`, ctx.memberToken, body);
    check(res, {
      'rbac v1 returned 200': (r) => r.status === 200,
      'rbac v1 strategy': (r) => r.status === 200 && r.json('strategy') === 'rbac',
    });
  });

  group('authorization model: abac/opa v2', () => {
    const res = postJson(`/api/v2/workspaces/${ctx.workspaceId}/authorization/decisions`, ctx.memberToken, body);
    check(res, {
      'abac v2 returned 200': (r) => r.status === 200,
      'abac v2 strategy': (r) => r.status === 200 && r.json('strategy') === 'abac-opa',
    });
  });
}

function authorizationEngines(ctx) {
  const body = authorizationRequest(ctx);
  for (const engine of AUTH_ENGINES) {
    group(`authorization engine: ${engine}`, () => {
      const res = postJson(`/api/benchmarks/authorization/${engine}/decisions`, ctx.memberToken, body);
      check(res, {
        [`${engine} returned 200`]: (r) => r.status === 200,
        [`${engine} strategy present`]: (r) => r.status === 200 && typeof r.json('strategy') === 'string',
      });
    });
  }
}

function cacheStrategies(ctx) {
  for (const strategy of CACHE_STRATEGIES) {
    const key = `k6-cache-${strategy}-${exec.vu.idInTest}-${exec.scenario.iterationInTest}`;

    group(`cache strategy: ${strategy}`, () => {
      const setRes = postJson(`/api/benchmarks/cache/${strategy}/entries`, ctx.ownerToken, {
        key,
        value: {
          source: 'k6',
          strategy,
          iteration: exec.scenario.iterationInTest,
        },
        ttlSeconds: 60,
      });
      check(setRes, { [`${strategy} cache set 200`]: (r) => r.status === 200 });

      const getRes = http.get(`${BASE_URL}/api/benchmarks/cache/${strategy}/entries/${encodeURIComponent(key)}`, authHeaders(ctx.ownerToken));
      check(getRes, {
        [`${strategy} cache get 200`]: (r) => r.status === 200,
        [`${strategy} cache hit`]: (r) => r.status === 200 && r.json('hit') === true,
      });

      const incrementRes = postJson(`/api/benchmarks/cache/${strategy}/counters/${encodeURIComponent(key)}-counter/increment`, ctx.ownerToken, {
        ttlSeconds: 60,
      });
      check(incrementRes, { [`${strategy} counter increment 200`]: (r) => r.status === 200 });
    });
  }
}

function rateLimitAlgorithms(ctx) {
  for (const algorithm of RATE_LIMIT_ALGORITHMS) {
    group(`rate limit algorithm: ${algorithm}`, () => {
      const key = `k6-rate-${algorithm}-${exec.vu.idInTest}-${exec.scenario.iterationInTest}`;
      const res = postJson(`/api/benchmarks/rate-limit/${algorithm}/decisions`, ctx.ownerToken, {
        key,
        limit: Number(__ENV.RATE_LIMIT || '1000'),
        windowSeconds: Number(__ENV.RATE_LIMIT_WINDOW_SECONDS || '60'),
      });
      check(res, {
        [`${algorithm} decision 200`]: (r) => r.status === 200,
        [`${algorithm} algorithm echoed`]: (r) => r.status === 200 && r.json('algorithm') === algorithm,
      });
    });
  }
}

function realtimePolling(ctx) {
  if (!REALTIME_VERSIONS.includes('v1')) {
    return;
  }
  group('realtime delivery: polling v1', () => {
    const res = http.get(`${BASE_URL}/api/v1/workspaces/${ctx.workspaceId}/events?since=0`, authHeaders(ctx.ownerToken));
    check(res, {
      'polling returned 200': (r) => r.status === 200,
      'polling cursor returned': (r) => r.status === 200 && typeof r.json('nextCursor') === 'number',
    });
  });
}

function analyticsStorage(ctx) {
  for (const version of ANALYTICS_VERSIONS) {
    group(`analytics storage: ${version}`, () => {
      const res = http.get(`${BASE_URL}/api/${version}/tenants/${ctx.tenantId}/analytics/events`, authHeaders(ctx.ownerToken));
      check(res, {
        [`analytics ${version} returned 200`]: (r) => r.status === 200,
        [`analytics ${version} array response`]: (r) => r.status === 200 && Array.isArray(r.json()),
      });
    });
  }
}

function documentSearch(ctx) {
  for (const version of SEARCH_VERSIONS) {
    group(`document search: ${version}`, () => {
      const res = http.get(`${BASE_URL}/api/${version}/workspaces/${ctx.workspaceId}/documents/search?query=k6`, authHeaders(ctx.ownerToken));
      check(res, {
        [`search ${version} returned 200`]: (r) => r.status === 200,
        [`search ${version} array response`]: (r) => r.status === 200 && Array.isArray(r.json()),
      });
    });
  }
}

function login(userId, workspaceId) {
  const res = postJson('/api/auth/login', null, { userId, workspaceId });
  expect(res, `login ${userId}`, 200);
  return res.json('token');
}

function authorizationRequest(ctx) {
  return {
    action: 'document:update',
    resourceType: 'document',
    resourceId: 'document-owned-by-member',
    resourceTenantId: ctx.tenantId,
    resourceOwnerUserId: ctx.memberUserId,
    attributes: {
      status: 'DRAFT',
      riskLevel: 'LOW',
    },
  };
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

function includesSuite(name) {
  return SUITE === 'all' || SUITE.split(',').map((item) => item.trim()).includes(name);
}

function listEnv(name, fallback) {
  return (__ENV[name] || fallback)
    .split(',')
    .map((item) => item.trim())
    .filter((item) => item.length > 0);
}

function expect(res, label, status) {
  if (res.status !== status) {
    throw new Error(`${label} expected ${status}, got ${res.status}: ${res.body}`);
  }
}
