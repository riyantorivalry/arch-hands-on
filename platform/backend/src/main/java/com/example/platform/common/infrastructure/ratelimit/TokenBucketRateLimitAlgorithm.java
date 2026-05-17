package com.example.platform.common.infrastructure.ratelimit;

import com.example.platform.common.infrastructure.cache.CacheService;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class TokenBucketRateLimitAlgorithm implements RateLimitAlgorithm {

    private static final String TOKENS = "tokens";
    private static final String LAST_REFILL_EPOCH_MILLIS = "lastRefillEpochMillis";

    private final CacheService cacheService;

    public TokenBucketRateLimitAlgorithm(CacheService cacheService) {
        this.cacheService = cacheService;
    }

    @Override
    public String name() {
        return "token-bucket";
    }

    @Override
    public synchronized RateLimitDecision check(String key, int limit, int windowSeconds) {
        RateLimitInputs.validate(key, limit, windowSeconds);
        Instant now = Instant.now();
        long nowMillis = now.toEpochMilli();
        String cacheKey = RateLimitInputs.cacheKey(name(), key);
        double refillPerSecond = (double) limit / windowSeconds;

        BucketState state = readState(cacheService.get(cacheKey), limit, nowMillis);
        double elapsedSeconds = Math.max(0, nowMillis - state.lastRefillEpochMillis()) / 1000.0;
        double tokens = Math.min(limit, state.tokens() + (elapsedSeconds * refillPerSecond));

        boolean allowed = tokens >= 1.0;
        if (allowed) {
            tokens -= 1.0;
        }

        cacheService.set(cacheKey, state(tokens, nowMillis), Math.max(windowSeconds * 2L, 1L));

        long remaining = Math.max(0, (long) Math.floor(tokens));
        long secondsUntilNextToken = tokens >= 1.0 ? 0 : (long) Math.ceil((1.0 - tokens) / refillPerSecond);
        long secondsUntilFull = (long) Math.ceil(Math.max(0, limit - tokens) / refillPerSecond);
        long retryAfterSeconds = allowed ? 0 : Math.max(1, secondsUntilNextToken);

        return new RateLimitDecision(name(), key, allowed, limit, remaining, retryAfterSeconds, secondsUntilFull, now);
    }

    private BucketState readState(Object value, int limit, long nowMillis) {
        if (value instanceof Map<?, ?> map) {
            return new BucketState(asDouble(map.get(TOKENS), limit), asLong(map.get(LAST_REFILL_EPOCH_MILLIS), nowMillis));
        }
        return new BucketState(limit, nowMillis);
    }

    private Map<String, Object> state(double tokens, long lastRefillEpochMillis) {
        Map<String, Object> state = new HashMap<>();
        state.put(TOKENS, tokens);
        state.put(LAST_REFILL_EPOCH_MILLIS, lastRefillEpochMillis);
        return state;
    }

    private double asDouble(Object value, double fallback) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return value == null ? fallback : Double.parseDouble(value.toString());
    }

    private long asLong(Object value, long fallback) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return value == null ? fallback : Long.parseLong(value.toString());
    }

    private record BucketState(double tokens, long lastRefillEpochMillis) {
    }
}
