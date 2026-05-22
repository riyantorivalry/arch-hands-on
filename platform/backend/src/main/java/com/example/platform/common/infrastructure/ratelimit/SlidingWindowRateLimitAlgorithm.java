package com.example.platform.common.infrastructure.ratelimit;

import com.example.platform.common.infrastructure.cache.CacheService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class SlidingWindowRateLimitAlgorithm implements RateLimitAlgorithm {

    private final CacheService cacheService;

    public SlidingWindowRateLimitAlgorithm(CacheService cacheService) {
        this.cacheService = cacheService;
    }

    @Override
    public String name() {
        return "sliding-window";
    }

    @Override
    public Mono<RateLimitDecision> check(String key, int limit, int windowSeconds) {
        RateLimitInputs.validate(key, limit, windowSeconds);
        Instant now = Instant.now();
        long nowMillis = now.toEpochMilli();
        long windowMillis = TimeUnit.SECONDS.toMillis(windowSeconds);
        long cutoffMillis = nowMillis - windowMillis;
        String cacheKey = RateLimitInputs.cacheKey(name(), key);

        return cacheService.get(cacheKey)
                .map(this::readTimestamps)
                .defaultIfEmpty(new ArrayList<>())
                .flatMap(existing -> {
                    List<Long> timestamps = existing.stream()
                            .filter(timestamp -> timestamp > cutoffMillis)
                            .sorted(Comparator.naturalOrder())
                            .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);

                    boolean allowed = timestamps.size() < limit;
                    if (allowed) {
                        timestamps.add(nowMillis);
                    }

                    long remaining = Math.max(0, limit - timestamps.size());
                    long resetSeconds = resetSeconds(timestamps, windowMillis, nowMillis);
                    long retryAfterSeconds = allowed ? 0 : Math.max(1, resetSeconds);
                    RateLimitDecision decision = new RateLimitDecision(name(), key, allowed, limit, remaining, retryAfterSeconds, resetSeconds, now);

                    return cacheService.set(cacheKey, timestamps, windowSeconds).thenReturn(decision);
                });
    }

    private List<Long> readTimestamps(Object value) {
        if (!(value instanceof List<?> values)) {
            return new ArrayList<>();
        }
        List<Long> timestamps = new ArrayList<>();
        for (Object item : values) {
            if (item instanceof Number number) {
                timestamps.add(number.longValue());
            } else if (item != null) {
                timestamps.add(Long.parseLong(item.toString()));
            }
        }
        return timestamps;
    }

    private long resetSeconds(List<Long> timestamps, long windowMillis, long nowMillis) {
        if (timestamps.isEmpty()) {
            return 0;
        }
        long resetMillis = timestamps.get(0) + windowMillis - nowMillis;
        return Math.max(0, (long) Math.ceil(resetMillis / 1000.0));
    }
}
