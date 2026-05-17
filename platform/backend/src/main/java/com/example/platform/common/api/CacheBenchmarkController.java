package com.example.platform.common.api;

import com.example.platform.common.infrastructure.cache.CacheBackend;
import com.example.platform.common.infrastructure.cache.CacheService;
import com.example.platform.common.web.RequestContexts;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/benchmarks/cache")
public class CacheBenchmarkController {

    private final CacheService cacheService;

    public CacheBenchmarkController(CacheService cacheService) {
        this.cacheService = cacheService;
    }

    @GetMapping("/strategy")
    public CacheStrategyResponse selectedStrategy() {
        RequestContexts.authenticated();
        return new CacheStrategyResponse(cacheService.mode());
    }

    @PostMapping("/{strategy}/entries")
    public CacheEntryResponse setEntry(
            @PathVariable String strategy,
            @Valid @RequestBody CacheSetRequest request
    ) {
        RequestContexts.authenticated();
        CacheBackend backend = cacheService.backend(strategy);
        if (request.ttlSeconds() == null) {
            backend.set(request.key(), request.value());
        } else {
            backend.set(request.key(), request.value(), request.ttlSeconds());
        }
        Object value = backend.get(request.key());
        return new CacheEntryResponse(backend.name(), request.key(), value != null, value, backend.getTtl(request.key()), Instant.now());
    }

    @GetMapping("/{strategy}/entries/{key}")
    public CacheEntryResponse getEntry(@PathVariable String strategy, @PathVariable String key) {
        RequestContexts.authenticated();
        CacheBackend backend = cacheService.backend(strategy);
        Object value = backend.get(key);
        return new CacheEntryResponse(backend.name(), key, value != null, value, backend.getTtl(key), Instant.now());
    }

    @PostMapping("/{strategy}/counters/{key}/increment")
    public CacheCounterResponse increment(
            @PathVariable String strategy,
            @PathVariable String key,
            @Valid @RequestBody CacheIncrementRequest request
    ) {
        RequestContexts.authenticated();
        CacheBackend backend = cacheService.backend(strategy);
        long value = backend.increment(key, request.ttlSeconds());
        return new CacheCounterResponse(backend.name(), key, value, backend.getTtl(key), Instant.now());
    }

    @DeleteMapping("/{strategy}/entries/{key}")
    public CacheEntryResponse deleteEntry(@PathVariable String strategy, @PathVariable String key) {
        RequestContexts.authenticated();
        CacheBackend backend = cacheService.backend(strategy);
        backend.delete(key);
        return new CacheEntryResponse(backend.name(), key, false, null, backend.getTtl(key), Instant.now());
    }

    public record CacheStrategyResponse(String selectedStrategy) {
    }

    public record CacheSetRequest(
            @NotBlank String key,
            Object value,
            Long ttlSeconds
    ) {
    }

    public record CacheIncrementRequest(Long ttlSeconds) {
        public CacheIncrementRequest {
            if (ttlSeconds == null || ttlSeconds <= 0) {
                ttlSeconds = 60L;
            }
        }
    }

    public record CacheEntryResponse(
            String strategy,
            String key,
            boolean hit,
            Object value,
            long ttlSeconds,
            Instant observedAt
    ) {
    }

    public record CacheCounterResponse(
            String strategy,
            String key,
            long value,
            long ttlSeconds,
            Instant observedAt
    ) {
    }
}
