package com.example.platform.common.infrastructure;

import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis caching service for Phase 2.
 * Provides simple cache operations with TTL support.
 */
@Component
@ConditionalOnClass(RedisTemplate.class)
public class CacheService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CacheService.class);

    private final RedisTemplate<String, Object> redisTemplate;

    public CacheService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Get value from cache.
     */
    public Object get(String key) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value != null) {
                LOGGER.debug("Cache hit: {}", key);
            }
            return value;
        } catch (Exception e) {
            LOGGER.warn("Error getting from cache: {}", key, e);
            return null;
        }
    }

    /**
     * Set value in cache with TTL.
     */
    public void set(String key, Object value, long ttlSeconds) {
        try {
            redisTemplate.opsForValue().set(key, value, ttlSeconds, TimeUnit.SECONDS);
            LOGGER.debug("Cache set: {} (TTL: {}s)", key, ttlSeconds);
        } catch (Exception e) {
            LOGGER.warn("Error setting cache: {}", key, e);
        }
    }

    /**
     * Set value in cache (no expiration).
     */
    public void set(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, value);
            LOGGER.debug("Cache set: {}", key);
        } catch (Exception e) {
            LOGGER.warn("Error setting cache: {}", key, e);
        }
    }

    /**
     * Delete key from cache.
     */
    public void delete(String key) {
        try {
            redisTemplate.delete(key);
            LOGGER.debug("Cache deleted: {}", key);
        } catch (Exception e) {
            LOGGER.warn("Error deleting from cache: {}", key, e);
        }
    }

    /**
     * Delete multiple keys from cache.
     */
    public void deletePattern(String pattern) {
        try {
            redisTemplate.delete(redisTemplate.keys(pattern));
            LOGGER.debug("Cache cleared for pattern: {}", pattern);
        } catch (Exception e) {
            LOGGER.warn("Error clearing cache pattern: {}", pattern, e);
        }
    }

    /**
     * Check if key exists in cache.
     */
    public boolean exists(String key) {
        try {
            Boolean exists = redisTemplate.hasKey(key);
            return exists != null && exists;
        } catch (Exception e) {
            LOGGER.warn("Error checking cache key: {}", key, e);
            return false;
        }
    }

    /**
     * Increment counter (for rate limiting).
     */
    public long increment(String key, long ttlSeconds) {
        try {
            Long count = redisTemplate.opsForValue().increment(key);
            if (count == 1) {
                // First increment, set expiration
                redisTemplate.expire(key, ttlSeconds, TimeUnit.SECONDS);
            }
            return count;
        } catch (Exception e) {
            LOGGER.warn("Error incrementing cache counter: {}", key, e);
            return 0;
        }
    }

    /**
     * Get remaining TTL for key (in seconds).
     */
    public long getTtl(String key) {
        try {
            Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
            return ttl != null ? ttl : -1;
        } catch (Exception e) {
            LOGGER.warn("Error getting TTL for key: {}", key, e);
            return -1;
        }
    }
}

