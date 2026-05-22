package com.example.platform.common.infrastructure.ratelimit;

import reactor.core.publisher.Mono;

public interface RateLimitAlgorithm {

    String name();

    Mono<RateLimitDecision> check(String key, int limit, int windowSeconds);
}
