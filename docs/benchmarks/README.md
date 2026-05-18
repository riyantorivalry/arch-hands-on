# Benchmarks

Store benchmark plans, scripts, raw outputs, and summary reports here.

## Quick start

See [BENCHMARK_TEMPLATE.md](./BENCHMARK_TEMPLATE.md) for the full report structure and guidelines.

## Current comparison areas

- Realtime delivery: polling vs SSE vs WebSocket
- Analytics storage: PostgreSQL JSON/JSONB-style storage vs MongoDB
- Authorization model: RBAC vs ABAC/OPA-style local evaluation
- Authorization policy engine: in-code vs OPA-style local vs Casbin-style local vs database-backed rules
- Cache strategy: Caffeine vs Redis vs Memcached
- Rate limiting algorithm: fixed window vs sliding window vs token bucket
- Read routing: primary-only fallback vs primary/replica routing

Branch-level experiments can add benchmark reports for REST vs gRPC, JSON vs Protobuf, tenant-per-schema isolation, or extracted service overhead when those branches are active.

## Expected structure

Each benchmark report should include:

- test plan (scenario, success criteria, environment setup)
- methodology (implementations being tested, load profile, tools)
- raw results (throughput, latency, resource utilization)
- analysis (performance characteristics, cost impact, trade-offs)
- decision (recommended approach and implementation plan)
- limitations and caveats

## How to contribute

1. Copy [BENCHMARK_TEMPLATE.md](./BENCHMARK_TEMPLATE.md) to a new file: `BENCHMARK-[date]-[subject].md`
2. Fill out each section, focusing on actionable insights
3. Link raw result files and test scripts in the appendix
4. Include graphs or comparison tables where helpful
5. Submit for peer review before publishing
