package com.example.platform.common.infrastructure.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class CaffeineCacheBackend implements CacheBackend {

    private final Cache<String, CacheEntry> cache;

    public CaffeineCacheBackend(
            @Value("${platform.feature.cache.caffeine.maximum-size:10000}") long maximumSize
    ) {
        this.cache = Caffeine.newBuilder()
                .maximumSize(maximumSize)
                .recordStats()
                .build();
    }

    @Override
    public String name() {
        return "caffeine";
    }

    @Override
    public Mono<Object> get(String key) {
        return Mono.fromSupplier(() -> {
            CacheEntry entry = cache.getIfPresent(key);
            if (entry == null) {
                return null;
            }
            if (entry.isExpired()) {
                cache.invalidate(key);
                return null;
            }
            return entry.value();
        });
    }

    @Override
    public Mono<Void> set(String key, Object value, long ttlSeconds) {
        return Mono.fromRunnable(() -> cache.put(key, new CacheEntry(value, expiresAt(ttlSeconds), null)));
    }

    @Override
    public Mono<Void> set(String key, Object value) {
        return Mono.fromRunnable(() -> cache.put(key, new CacheEntry(value, null, null)));
    }

    @Override
    public Mono<Void> delete(String key) {
        return Mono.fromRunnable(() -> cache.invalidate(key));
    }

    @Override
    public Mono<Void> deletePattern(String pattern) {
        return Mono.fromRunnable(() -> {
            String regex = pattern.replace(".", "\\.").replace("*", ".*");
            cache.asMap().keySet().removeIf(key -> key.matches(regex));
        });
    }

    @Override
    public Mono<Boolean> exists(String key) {
        return get(key).map(value -> true).defaultIfEmpty(false);
    }

    @Override
    public Mono<Long> increment(String key, long ttlSeconds) {
        return Mono.fromSupplier(() -> {
            CacheEntry entry = cache.asMap().compute(key, (ignored, existing) -> {
                if (existing == null || existing.isExpired()) {
                    return new CacheEntry(null, expiresAt(ttlSeconds), new AtomicLong(1));
                }
                AtomicLong counter = existing.counter();
                if (counter == null) {
                    counter = new AtomicLong(asLong(existing.value()));
                }
                counter.incrementAndGet();
                return new CacheEntry(null, existing.expiresAt(), counter);
            });
            return entry.counter().get();
        });
    }

    @Override
    public Mono<Long> getTtl(String key) {
        return Mono.fromSupplier(() -> {
            CacheEntry entry = cache.getIfPresent(key);
            if (entry == null || entry.expiresAt() == null) {
                return -1L;
            }
            long ttlMillis = entry.expiresAt().toEpochMilli() - Instant.now().toEpochMilli();
            return Math.max(0, TimeUnit.MILLISECONDS.toSeconds(ttlMillis));
        });
    }

    private Instant expiresAt(long ttlSeconds) {
        return ttlSeconds <= 0 ? null : Instant.now().plusSeconds(ttlSeconds);
    }

    private long asLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value == null) {
            return 0;
        }
        return Long.parseLong(value.toString());
    }

    private record CacheEntry(Object value, Instant expiresAt, AtomicLong counter) {

        boolean isExpired() {
            return expiresAt != null && !expiresAt.isAfter(Instant.now());
        }
    }
}
