# Benchmark Report Template

## Executive Summary

**Objective:** [What are we comparing? e.g., REST vs gRPC for channel messaging]

**Conclusion:** [Key finding in one sentence]

**Date:** YYYY-MM-DD  
**Ran by:** [Name/Team]  
**Test environment:** [Local/staging/cloud region] | Kubernetes/Docker/Binaries | Hardware specs

---

## Test Plan

### Scenario
[Describe the specific workflow being tested. Include actor count, message pattern, payload size, etc.]

Example:
- 100 concurrent users
- Each posts 10 messages over 2 minutes to a channel
- Average message payload: ~2 KB
- Total messages: 1000

### Success Criteria
[What constitutes a passing result?]

- p50 latency < 100ms
- p95 latency < 500ms
- p99 latency < 1000ms
- Error rate < 0.1%

### Test environment setup
[Any special configuration, warm-up procedures, data fixtures, etc.]

---

## Methodology

### Protocol/Implementation A: [Name]
- Implementation approach
- Configuration details
- Assumptions

### Protocol/Implementation B: [Name]
- Implementation approach
- Configuration details
- Assumptions

### Load profile
- Duration
- Ramp-up strategy
- Steady-state hold time
- Cool-down time
- Metric collection interval

### Tools used
- Load generator: [tool, version]
- Monitoring stack: [Prometheus, Grafana, etc.]
- Analysis tools: [R, Python, etc.]

---

## Raw Results

### Throughput (requests/sec)

| Metric | Protocol A | Protocol B | Delta | Winner |
|--------|-----------|-----------|-------|--------|
| Mean | X | Y | % | - |
| p50 | X | Y | % | - |
| p95 | X | Y | % | - |
| p99 | X | Y | % | - |

### Latency (milliseconds)

| Percentile | Protocol A | Protocol B | Delta |
|-----------|-----------|-----------|-------|
| p50 | X | Y | % |
| p90 | X | Y | % |
| p95 | X | Y | % |
| p99 | X | Y | % |
| p99.9 | X | Y | % |

### Resource utilization

| Resource | Protocol A | Protocol B | Notes |
|----------|-----------|-----------|-------|
| CPU (avg %) | X | Y | - |
| Memory (peak MB) | X | Y | - |
| Network (avg Mbps) | X | Y | - |
| Disk I/O | X | Y | - |

### Error distribution (if any)

| Error type | Count | Rate | Pattern |
|-----------|-------|------|---------|
| - | - | - | - |

### Observations during test

- What happened at which point?
- Did either implementation hit any limits?
- Unexpected behavior?
- GC pauses, connection resets, etc.?

---

## Analysis

### Performance characteristics

**Protocol A:** [Key strengths and weaknesses]

**Protocol B:** [Key strengths and weaknesses]

### Cost impact

- Infrastructure cost per 1M operations
- Operational overhead (monitoring, debugging, etc.)
- Developer productivity implications

### Trade-off summary

| Dimension | Protocol A | Protocol B | Recommendation |
|-----------|-----------|-----------|-----------------|
| Latency | - | - | - |
| Throughput | - | - | - |
| Payload size | - | - | - |
| Complexity | - | - | - |
| Developer experience | - | - | - |

### Scalability extrapolation

- At 10x load, which implementation is likely to scale better?
- Which resource becomes the bottleneck?
- Estimated cost at production scale?

---

## Limitations and caveats

- Single-node vs distributed testing?
- Warm cache assumptions?
- Network latency simulation included?
- Any unrealistic test conditions?
- Production traffic patterns we didn't model?

---

## Decision

### Recommended approach: [Protocol A / Protocol B / Hybrid]

### Rationale
[Explain why, given the results and constraints]

### Implementation plan
- Phase 1: [What gets deployed first?]
- Phase 2: [What comes next?]
- Fallback: [What's the rollback plan?]

### Monitoring and tuning
- Which metrics do we watch post-deployment?
- When do we reconsider this decision?
- Tuning parameters we'll adjust?

---

## Appendix

### Test script location
[Link to k6/JMeter/Vegeta script]

### Raw data files
[S3/repo link to CSV, JSON, or binaries]

### Grafana dashboard
[Link or screenshot]

### Team notes
[Slack threads, discussion records, etc.]
