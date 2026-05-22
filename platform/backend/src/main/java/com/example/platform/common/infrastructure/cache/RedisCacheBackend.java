package com.example.platform.common.infrastructure.cache;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class RedisCacheBackend implements CacheBackend {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisCacheBackend.class);

    private final ReactiveRedisTemplate<String, Object> redisTemplate;

    public RedisCacheBackend(ReactiveRedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public String name() {
        return "redis";
    }

    @Override
    public Mono<Object> get(String key) {
        return redisTemplate.opsForValue().get(key)
                .doOnError(exception -> LOGGER.warn("Error getting Redis cache key: {}", key, exception))
                .onErrorResume(exception -> Mono.empty());
    }

    @Override
    public Mono<Void> set(String key, Object value, long ttlSeconds) {
        return redisTemplate.opsForValue().set(key, value, Duration.ofSeconds(ttlSeconds))
                .doOnError(exception -> LOGGER.warn("Error setting Redis cache key: {}", key, exception))
                .onErrorResume(exception -> Mono.just(false))
                .then();
    }

    @Override
    public Mono<Void> set(String key, Object value) {
        return redisTemplate.opsForValue().set(key, value)
                .doOnError(exception -> LOGGER.warn("Error setting Redis cache key: {}", key, exception))
                .onErrorResume(exception -> Mono.just(false))
                .then();
    }

    @Override
    public Mono<Void> delete(String key) {
        return redisTemplate.delete(key)
                .doOnError(exception -> LOGGER.warn("Error deleting Redis cache key: {}", key, exception))
                .onErrorResume(exception -> Mono.just(0L))
                .then();
    }

    @Override
    public Mono<Void> deletePattern(String pattern) {
        return redisTemplate.keys(pattern)
                .collectList()
                .flatMap(keys -> keys.isEmpty() ? Mono.just(0L) : redisTemplate.delete(keys.toArray(String[]::new)))
                .doOnError(exception -> LOGGER.warn("Error deleting Redis cache pattern: {}", pattern, exception))
                .onErrorResume(exception -> Mono.just(0L))
                .then();
    }

    @Override
    public Mono<Boolean> exists(String key) {
        return redisTemplate.hasKey(key)
                .doOnError(exception -> LOGGER.warn("Error checking Redis cache key: {}", key, exception))
                .onErrorReturn(false);
    }

    @Override
    public Mono<Long> increment(String key, long ttlSeconds) {
        return redisTemplate.opsForValue().increment(key)
                .flatMap(count -> count == 1
                        ? redisTemplate.expire(key, Duration.ofSeconds(ttlSeconds)).thenReturn(count)
                        : Mono.just(count))
                .doOnError(exception -> LOGGER.warn("Error incrementing Redis cache key: {}", key, exception))
                .onErrorReturn(0L);
    }

    @Override
    public Mono<Long> getTtl(String key) {
        return redisTemplate.getExpire(key)
                .map(Duration::getSeconds)
                .defaultIfEmpty(-1L)
                .doOnError(exception -> LOGGER.warn("Error getting Redis cache TTL: {}", key, exception))
                .onErrorReturn(-1L);
    }
}
