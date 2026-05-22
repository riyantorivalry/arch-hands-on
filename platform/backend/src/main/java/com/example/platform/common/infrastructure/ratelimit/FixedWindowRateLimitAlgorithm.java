package com.example.platform.common.infrastructure.ratelimit;

import com.example.platform.common.infrastructure.cache.CacheService;
import java.time.Instant;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class FixedWindowRateLimitAlgorithm implements RateLimitAlgorithm {

    private final CacheService cacheService;

    public FixedWindowRateLimitAlgorithm(CacheService cacheService) {
        this.cacheService = cacheService;
    }

    @Override
    public String name() {
        return "fixed-window";
    }

    @Override
    public Mono<RateLimitDecision> check(String key, int limit, int windowSeconds) {
        RateLimitInputs.validate(key, limit, windowSeconds);
        Instant now = Instant.now();
        long epochSecond = now.getEpochSecond();
        long windowStart = (epochSecond / windowSeconds) * windowSeconds;
        long windowEnd = windowStart + windowSeconds;
        String cacheKey = RateLimitInputs.cacheKey(name(), key) + ":" + windowStart;

        return cacheService.increment(cacheKey, windowSeconds + 1L)
                .map(count -> {
                    boolean allowed = count <= limit;
                    long remaining = Math.max(0, limit - count);
                    long resetSeconds = Math.max(0, windowEnd - epochSecond);
                    long retryAfterSeconds = allowed ? 0 : Math.max(1, resetSeconds);
                    return new RateLimitDecision(name(), key, allowed, limit, remaining, retryAfterSeconds, resetSeconds, now);
                });
    }
}
