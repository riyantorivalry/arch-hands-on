import { Counter, Rate, Trend } from 'k6/metrics';

const DEFAULT_REPORT_PATH = 'k6-report.html';

export function createEndpointRegistry(labels) {
  const uniqueLabels = unique(withUnknownEndpoint(labels));
  const entries = [];
  const byLabel = {};

  for (let index = 0; index < uniqueLabels.length; index += 1) {
    const label = uniqueLabels[index];
    const metricBase = `api_${leftPad(index + 1, 3)}`;
    const entry = {
      label,
      durationName: `${metricBase}_duration`,
      requestsName: `${metricBase}_requests`,
      failedName: `${metricBase}_failed`,
    };
    entry.duration = new Trend(entry.durationName, true);
    entry.requests = new Counter(entry.requestsName);
    entry.failed = new Rate(entry.failedName);
    entries.push(entry);
    byLabel[label] = entry;
  }

  return {
    entries,
    record(label, res) {
      const entry = byLabel[label] || byLabel[UNKNOWN_ENDPOINT_LABEL];
      const duration = res && res.timings && typeof res.timings.duration === 'number' ? res.timings.duration : 0;
      const status = res && typeof res.status === 'number' ? res.status : 0;

      entry.duration.add(duration);
      entry.requests.add(1);
      entry.failed.add(status < 200 || status >= 400 || Boolean(res && res.error));
      return res;
    },
  };
}

export const UNKNOWN_ENDPOINT_LABEL = 'UNCLASSIFIED API REQUEST';

export function withUnknownEndpoint(labels) {
  return labels.concat([UNKNOWN_ENDPOINT_LABEL]);
}

export function createHtmlSummary(data, registry, config) {
  const reportPath = config.reportPath || DEFAULT_REPORT_PATH;
  const rows = endpointRows(data, registry);
  const thresholds = thresholdRows(data);
  const cards = summaryCards(data, rows, thresholds);
  const metadata = config.metadata || {};
  const thresholdFailed = thresholds.some((item) => !item.ok);
  const statusLabel = thresholdFailed ? 'Thresholds failed' : 'Thresholds passed';
  const statusClass = thresholdFailed ? 'status-fail' : 'status-pass';
  const html = renderHtml({
    title: config.title || 'k6 API Performance Report',
    generatedAt: new Date().toISOString(),
    metadata,
    rows,
    thresholds,
    cards,
    statusLabel,
    statusClass,
    latencyBudgetMs: config.latencyBudgetMs,
  });

  const outputs = {
    stdout: consoleSummary(config.title || 'k6 API Performance Report', reportPath, cards, rows, thresholdFailed),
  };
  outputs[reportPath] = html;

  if (config.jsonReportPath) {
    outputs[config.jsonReportPath] = JSON.stringify(data, null, 2);
  }

  return outputs;
}

function endpointRows(data, registry) {
  const rows = [];

  for (const entry of registry.entries) {
    const requestMetric = data.metrics[entry.requestsName];
    const requests = metricNumber(requestMetric, 'count');
    if (!requests) {
      continue;
    }

    const durationMetric = data.metrics[entry.durationName];
    const failedMetric = data.metrics[entry.failedName];
    const values = durationMetric && durationMetric.values ? durationMetric.values : {};
    rows.push({
      label: entry.label,
      method: endpointMethod(entry.label),
      path: endpointPath(entry.label),
      count: requests,
      failureRate: metricNumber(failedMetric, 'rate'),
      avg: metricNumber(durationMetric, 'avg'),
      min: metricNumber(durationMetric, 'min'),
      median: metricNumber(durationMetric, 'med'),
      p90: valueNumber(values['p(90)']),
      p95: valueNumber(values['p(95)']),
      p99: valueNumber(values['p(99)']),
      max: metricNumber(durationMetric, 'max'),
    });
  }

  return rows.sort((left, right) => right.p95 - left.p95);
}

function thresholdRows(data) {
  const rows = [];
  for (const metricName of Object.keys(data.metrics).sort()) {
    const metric = data.metrics[metricName];
    if (!metric.thresholds) {
      continue;
    }
    for (const expression of Object.keys(metric.thresholds)) {
      const threshold = metric.thresholds[expression];
      rows.push({
        metricName,
        expression,
        ok: typeof threshold === 'boolean' ? threshold : Boolean(threshold.ok),
      });
    }
  }
  return rows;
}

function summaryCards(data, rows, thresholds) {
  const globalDuration = data.metrics.http_req_duration;
  const globalFailed = data.metrics.http_req_failed;
  const globalRequests = data.metrics.http_reqs;
  const slowest = rows.length > 0 ? rows[0] : null;

  return [
    {
      label: 'Requests',
      value: formatNumber(metricNumber(globalRequests, 'count')),
      detail: `${formatNumber(rows.length)} instrumented APIs`,
    },
    {
      label: 'Failure rate',
      value: formatPercent(metricNumber(globalFailed, 'rate')),
      detail: 'HTTP requests with failed status',
    },
    {
      label: 'Overall p95',
      value: formatMs(metricNumber(globalDuration, 'p(95)')),
      detail: `p99 ${formatMs(metricNumber(globalDuration, 'p(99)'))}`,
    },
    {
      label: 'Slowest API p95',
      value: slowest ? formatMs(slowest.p95) : 'n/a',
      detail: slowest ? slowest.label : 'No endpoint samples',
    },
    {
      label: 'Thresholds',
      value: `${thresholds.filter((item) => item.ok).length}/${thresholds.length}`,
      detail: thresholds.some((item) => !item.ok) ? 'One or more failed' : 'All passed',
    },
  ];
}

function renderHtml(view) {
  return `<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>${escapeHtml(view.title)}</title>
  <style>
    :root {
      color-scheme: light;
      --bg: #f6f8fb;
      --panel: #ffffff;
      --text: #172033;
      --muted: #5d697d;
      --border: #d9e0ea;
      --good: #0f7b50;
      --good-bg: #e7f6ef;
      --bad: #b42318;
      --bad-bg: #fdecea;
      --warn: #9a6700;
      --warn-bg: #fff4d6;
      --head: #eef2f7;
    }
    * { box-sizing: border-box; }
    body {
      margin: 0;
      background: var(--bg);
      color: var(--text);
      font: 14px/1.45 Arial, Helvetica, sans-serif;
    }
    header {
      padding: 28px 32px 18px;
      border-bottom: 1px solid var(--border);
      background: var(--panel);
    }
    h1 {
      margin: 0 0 8px;
      font-size: 28px;
      line-height: 1.2;
      letter-spacing: 0;
    }
    h2 {
      margin: 28px 0 12px;
      font-size: 18px;
      letter-spacing: 0;
    }
    main { padding: 24px 32px 40px; }
    .meta, .cards {
      display: grid;
      gap: 12px;
    }
    .meta {
      grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
      margin-top: 18px;
    }
    .meta div, .card {
      background: var(--panel);
      border: 1px solid var(--border);
      border-radius: 8px;
      padding: 12px 14px;
    }
    .meta span, .card span {
      display: block;
      color: var(--muted);
      font-size: 12px;
      text-transform: uppercase;
    }
    .meta strong, .card strong {
      display: block;
      margin-top: 3px;
      overflow-wrap: anywhere;
    }
    .cards {
      grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
    }
    .card strong {
      font-size: 24px;
      line-height: 1.2;
    }
    .card p {
      margin: 5px 0 0;
      color: var(--muted);
      overflow-wrap: anywhere;
    }
    .status {
      display: inline-block;
      border-radius: 999px;
      padding: 4px 10px;
      font-weight: 700;
      font-size: 12px;
    }
    .status-pass { color: var(--good); background: var(--good-bg); }
    .status-fail { color: var(--bad); background: var(--bad-bg); }
    .table-wrap {
      overflow-x: auto;
      background: var(--panel);
      border: 1px solid var(--border);
      border-radius: 8px;
    }
    table {
      width: 100%;
      border-collapse: collapse;
      min-width: 980px;
    }
    th, td {
      border-bottom: 1px solid var(--border);
      padding: 10px 12px;
      text-align: right;
      vertical-align: top;
      white-space: nowrap;
    }
    th {
      position: sticky;
      top: 0;
      background: var(--head);
      color: #253248;
      font-size: 12px;
      text-transform: uppercase;
    }
    td:first-child, th:first-child,
    td:nth-child(2), th:nth-child(2) {
      text-align: left;
    }
    td:nth-child(2) {
      white-space: normal;
      min-width: 340px;
      overflow-wrap: anywhere;
    }
    tbody tr:last-child td { border-bottom: 0; }
    .method {
      display: inline-block;
      min-width: 48px;
      text-align: center;
      border-radius: 4px;
      padding: 2px 6px;
      font-weight: 700;
      background: #e8edf5;
    }
    .ok { color: var(--good); font-weight: 700; }
    .fail { color: var(--bad); font-weight: 700; }
    .warn {
      color: var(--warn);
      background: var(--warn-bg);
      border-radius: 4px;
      padding: 2px 6px;
      font-weight: 700;
    }
    footer {
      padding: 18px 32px;
      color: var(--muted);
      border-top: 1px solid var(--border);
    }
    @media (max-width: 720px) {
      header, main, footer { padding-left: 16px; padding-right: 16px; }
      h1 { font-size: 22px; }
      .card strong { font-size: 20px; }
    }
  </style>
</head>
<body>
  <header>
    <h1>${escapeHtml(view.title)}</h1>
    <span class="status ${view.statusClass}">${escapeHtml(view.statusLabel)}</span>
    <section class="meta">
      ${metadataHtml(view.metadata, view.generatedAt)}
    </section>
  </header>
  <main>
    <section class="cards">
      ${cardsHtml(view.cards)}
    </section>
    <h2>API Response Times</h2>
    <div class="table-wrap">
      <table>
        <thead>
          <tr>
            <th>Method</th>
            <th>API</th>
            <th>Requests</th>
            <th>Fail rate</th>
            <th>Avg</th>
            <th>Min</th>
            <th>Median</th>
            <th>p90</th>
            <th>p95</th>
            <th>p99</th>
            <th>Max</th>
          </tr>
        </thead>
        <tbody>
          ${apiRowsHtml(view.rows, view.latencyBudgetMs)}
        </tbody>
      </table>
    </div>
    <h2>Thresholds</h2>
    <div class="table-wrap">
      <table>
        <thead>
          <tr>
            <th>Metric</th>
            <th>Expression</th>
            <th>Status</th>
          </tr>
        </thead>
        <tbody>
          ${thresholdRowsHtml(view.thresholds)}
        </tbody>
      </table>
    </div>
  </main>
  <footer>Generated by k6 handleSummary at ${escapeHtml(view.generatedAt)}.</footer>
</body>
</html>
`;
}

function metadataHtml(metadata, generatedAt) {
  const merged = Object.assign({ 'Generated at': generatedAt }, metadata);
  return Object.keys(merged).map((key) => {
    return `<div><span>${escapeHtml(key)}</span><strong>${escapeHtml(String(merged[key]))}</strong></div>`;
  }).join('');
}

function cardsHtml(cards) {
  return cards.map((card) => {
    return `<article class="card"><span>${escapeHtml(card.label)}</span><strong>${escapeHtml(card.value)}</strong><p>${escapeHtml(card.detail)}</p></article>`;
  }).join('');
}

function apiRowsHtml(rows, latencyBudgetMs) {
  if (rows.length === 0) {
    return '<tr><td colspan="11">No endpoint samples were recorded.</td></tr>';
  }
  return rows.map((row) => {
    const failureClass = row.failureRate > 0 ? 'fail' : 'ok';
    const p95 = latencyBudgetMs && row.p95 > latencyBudgetMs
      ? `<span class="warn">${formatMs(row.p95)}</span>`
      : formatMs(row.p95);
    return `<tr>
      <td><span class="method">${escapeHtml(row.method)}</span></td>
      <td>${escapeHtml(row.path)}</td>
      <td>${formatNumber(row.count)}</td>
      <td class="${failureClass}">${formatPercent(row.failureRate)}</td>
      <td>${formatMs(row.avg)}</td>
      <td>${formatMs(row.min)}</td>
      <td>${formatMs(row.median)}</td>
      <td>${formatMs(row.p90)}</td>
      <td>${p95}</td>
      <td>${formatMs(row.p99)}</td>
      <td>${formatMs(row.max)}</td>
    </tr>`;
  }).join('');
}

function thresholdRowsHtml(rows) {
  if (rows.length === 0) {
    return '<tr><td colspan="3">No thresholds were configured.</td></tr>';
  }
  return rows.map((row) => {
    const statusClass = row.ok ? 'ok' : 'fail';
    const statusText = row.ok ? 'Pass' : 'Fail';
    return `<tr>
      <td>${escapeHtml(row.metricName)}</td>
      <td>${escapeHtml(row.expression)}</td>
      <td class="${statusClass}">${statusText}</td>
    </tr>`;
  }).join('');
}

function consoleSummary(title, reportPath, cards, rows, thresholdFailed) {
  const slowest = rows.length > 0 ? rows[0] : null;
  const parts = [
    '',
    `${title}`,
    `HTML report: ${reportPath}`,
    `Status: ${thresholdFailed ? 'thresholds failed' : 'thresholds passed'}`,
  ];
  for (const card of cards) {
    parts.push(`${card.label}: ${card.value}`);
  }
  if (slowest) {
    parts.push(`Slowest API by p95: ${slowest.label} (${formatMs(slowest.p95)})`);
  }
  parts.push('');
  return `${parts.join('\n')}\n`;
}

function endpointMethod(label) {
  const match = /^([A-Z]+)\s+/.exec(label);
  return match ? match[1] : 'API';
}

function endpointPath(label) {
  return label.replace(/^[A-Z]+\s+/, '');
}

function metricNumber(metric, key) {
  if (!metric || !metric.values) {
    return 0;
  }
  return valueNumber(metric.values[key]);
}

function valueNumber(value) {
  return typeof value === 'number' && isFinite(value) ? value : 0;
}

function formatMs(value) {
  if (!value && value !== 0) {
    return 'n/a';
  }
  return `${formatNumber(value, value >= 100 ? 0 : 2)} ms`;
}

function formatPercent(value) {
  return `${formatNumber(value * 100, value > 0 && value < 0.01 ? 3 : 2)}%`;
}

function formatNumber(value, decimals) {
  const fixedDecimals = typeof decimals === 'number' ? decimals : 0;
  if (!value && value !== 0) {
    return '0';
  }
  const rounded = Number(value).toFixed(fixedDecimals);
  const parts = rounded.split('.');
  parts[0] = parts[0].replace(/\B(?=(\d{3})+(?!\d))/g, ',');
  return parts.join('.');
}

function unique(values) {
  const seen = {};
  const result = [];
  for (const value of values) {
    if (!seen[value]) {
      seen[value] = true;
      result.push(value);
    }
  }
  return result;
}

function leftPad(value, width) {
  const text = String(value);
  if (text.length >= width) {
    return text;
  }
  return `${new Array(width - text.length + 1).join('0')}${text}`;
}

function escapeHtml(value) {
  return String(value)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}
