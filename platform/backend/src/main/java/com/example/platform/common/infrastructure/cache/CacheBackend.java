package com.example.platform.common.infrastructure.cache;

import reactor.core.publisher.Mono;

public interface CacheBackend {

    String name();

    Mono<Object> get(String key);

    Mono<Void> set(String key, Object value, long ttlSeconds);

    Mono<Void> set(String key, Object value);

    Mono<Void> delete(String key);

    Mono<Void> deletePattern(String pattern);

    Mono<Boolean> exists(String key);

    Mono<Long> increment(String key, long ttlSeconds);

    Mono<Long> getTtl(String key);
}
