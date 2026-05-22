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
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/benchmarks/cache")
public class CacheBenchmarkController {

    private final CacheService cacheService;

    public CacheBenchmarkController(CacheService cacheService) {
        this.cacheService = cacheService;
    }

    @GetMapping("/strategy")
    public Mono<CacheStrategyResponse> selectedStrategy() {
        return RequestContexts.authenticatedReactive()
                .thenReturn(new CacheStrategyResponse(cacheService.mode()));
    }

    @PostMapping("/{strategy}/entries")
    public Mono<CacheEntryResponse> setEntry(
            @PathVariable String strategy,
            @Valid @RequestBody CacheSetRequest request
    ) {
        return RequestContexts.authenticatedReactive()
                .then(Mono.defer(() -> {
                    CacheBackend backend = cacheService.backend(strategy);
                    Mono<Void> write = request.ttlSeconds() == null
                            ? backend.set(request.key(), request.value())
                            : backend.set(request.key(), request.value(), request.ttlSeconds());
                    return write.then(responseFor(backend, request.key()));
                }));
    }

    @GetMapping("/{strategy}/entries/{key}")
    public Mono<CacheEntryResponse> getEntry(@PathVariable String strategy, @PathVariable String key) {
        return RequestContexts.authenticatedReactive()
                .then(Mono.defer(() -> responseFor(cacheService.backend(strategy), key)));
    }

    @PostMapping("/{strategy}/counters/{key}/increment")
    public Mono<CacheCounterResponse> increment(
            @PathVariable String strategy,
            @PathVariable String key,
            @Valid @RequestBody CacheIncrementRequest request
    ) {
        return RequestContexts.authenticatedReactive()
                .then(Mono.defer(() -> {
                    CacheBackend backend = cacheService.backend(strategy);
                    return backend.increment(key, request.ttlSeconds())
                            .zipWith(backend.getTtl(key))
                            .map(tuple -> new CacheCounterResponse(backend.name(), key, tuple.getT1(), tuple.getT2(), Instant.now()));
                }));
    }

    @DeleteMapping("/{strategy}/entries/{key}")
    public Mono<CacheEntryResponse> deleteEntry(@PathVariable String strategy, @PathVariable String key) {
        return RequestContexts.authenticatedReactive()
                .then(Mono.defer(() -> {
                    CacheBackend backend = cacheService.backend(strategy);
                    return backend.delete(key)
                            .then(backend.getTtl(key))
                            .map(ttl -> new CacheEntryResponse(backend.name(), key, false, null, ttl, Instant.now()));
                }));
    }

    private Mono<CacheEntryResponse> responseFor(CacheBackend backend, String key) {
        return backend.get(key)
                .map(value -> new CacheEntryResponse(backend.name(), key, true, value, -1, Instant.now()))
                .defaultIfEmpty(new CacheEntryResponse(backend.name(), key, false, null, -1, Instant.now()))
                .flatMap(response -> backend.getTtl(key)
                        .map(ttl -> new CacheEntryResponse(
                                response.strategy(),
                                response.key(),
                                response.hit(),
                                response.value(),
                                ttl,
                                response.observedAt()
                        )));
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
