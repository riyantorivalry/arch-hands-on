package com.example.platform.common.infrastructure.ratelimit;

import java.time.Instant;

public record RateLimitDecision(
        String algorithm,
        String key,
        boolean allowed,
        int limit,
        long remaining,
        long retryAfterSeconds,
        long resetSeconds,
        Instant observedAt
) {
}
