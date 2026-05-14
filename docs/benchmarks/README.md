# Benchmarks

Store benchmark plans, scripts, raw outputs, and summary reports here.

## Quick start

See [BENCHMARK_TEMPLATE.md](./BENCHMARK_TEMPLATE.md) for the full report structure and guidelines.

## Planned comparison areas

- REST vs gRPC
- JSON vs Protobuf
- PostgreSQL vs MongoDB for selected workflows
- WebSocket vs SSE
- monolith vs extracted service overhead

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
