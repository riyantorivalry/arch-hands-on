package com.example.platform.common.infrastructure.ratelimit;

public interface RateLimitAlgorithm {

    String name();

    RateLimitDecision check(String key, int limit, int windowSeconds);
}
