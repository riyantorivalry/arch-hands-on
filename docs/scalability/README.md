# Scalability

This directory stores scaling strategy and capacity analysis.

## Current analysis topics

- tenant growth model
- concurrency assumptions
- cache strategy across Caffeine, Redis, and Memcached
- rate limiting behavior across fixed window, sliding window, and token bucket
- realtime fanout across polling, SSE, and WebSocket
- analytics storage growth across PostgreSQL JSON/JSONB-style storage and MongoDB
- authorization policy evaluation cost across in-code, local policy, and database-backed rules
- PostgreSQL read routing and replica lag tolerance
- queue buffering strategy
- autoscaling thresholds
- regional expansion strategy
- cost/performance tradeoffs
