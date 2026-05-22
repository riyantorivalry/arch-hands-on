package com.example.platform.common.infrastructure.cache;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class CacheService {

    private final List<CacheBackend> backends;
    private final String mode;

    public CacheService(
            List<CacheBackend> backends,
            @Value("${platform.feature.cache.mode:caffeine}") String mode
    ) {
        this.backends = backends;
        this.mode = normalize(mode);
    }

    public String mode() {
        return mode;
    }

    public Mono<Object> get(String key) {
        return selected().get(key);
    }

    public Mono<Void> set(String key, Object value, long ttlSeconds) {
        return selected().set(key, value, ttlSeconds);
    }

    public Mono<Void> set(String key, Object value) {
        return selected().set(key, value);
    }

    public Mono<Void> delete(String key) {
        return selected().delete(key);
    }

    public Mono<Void> deletePattern(String pattern) {
        return selected().deletePattern(pattern);
    }

    public Mono<Boolean> exists(String key) {
        return selected().exists(key);
    }

    public Mono<Long> increment(String key, long ttlSeconds) {
        return selected().increment(key, ttlSeconds);
    }

    public Mono<Long> getTtl(String key) {
        return selected().getTtl(key);
    }

    public CacheBackend backend(String strategy) {
        String normalized = normalize(strategy);
        return backends.stream()
                .filter(backend -> backend.name().equals(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported cache strategy: " + strategy));
    }

    private CacheBackend selected() {
        return backend(mode);
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "caffeine";
        }
        return value.trim().toLowerCase();
    }
}
