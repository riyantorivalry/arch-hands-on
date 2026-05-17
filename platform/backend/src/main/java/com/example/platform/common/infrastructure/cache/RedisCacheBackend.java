package com.example.platform.common.infrastructure.cache;

import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisCacheBackend implements CacheBackend {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisCacheBackend.class);

    private final RedisTemplate<String, Object> redisTemplate;

    public RedisCacheBackend(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public String name() {
        return "redis";
    }

    @Override
    public Object get(String key) {
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (Exception exception) {
            LOGGER.warn("Error getting Redis cache key: {}", key, exception);
            return null;
        }
    }

    @Override
    public void set(String key, Object value, long ttlSeconds) {
        try {
            redisTemplate.opsForValue().set(key, value, ttlSeconds, TimeUnit.SECONDS);
        } catch (Exception exception) {
            LOGGER.warn("Error setting Redis cache key: {}", key, exception);
        }
    }

    @Override
    public void set(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, value);
        } catch (Exception exception) {
            LOGGER.warn("Error setting Redis cache key: {}", key, exception);
        }
    }

    @Override
    public void delete(String key) {
        try {
            redisTemplate.delete(key);
        } catch (Exception exception) {
            LOGGER.warn("Error deleting Redis cache key: {}", key, exception);
        }
    }

    @Override
    public void deletePattern(String pattern) {
        try {
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception exception) {
            LOGGER.warn("Error deleting Redis cache pattern: {}", pattern, exception);
        }
    }

    @Override
    public boolean exists(String key) {
        try {
            Boolean exists = redisTemplate.hasKey(key);
            return exists != null && exists;
        } catch (Exception exception) {
            LOGGER.warn("Error checking Redis cache key: {}", key, exception);
            return false;
        }
    }

    @Override
    public long increment(String key, long ttlSeconds) {
        try {
            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1) {
                redisTemplate.expire(key, ttlSeconds, TimeUnit.SECONDS);
            }
            return count == null ? 0 : count;
        } catch (Exception exception) {
            LOGGER.warn("Error incrementing Redis cache key: {}", key, exception);
            return 0;
        }
    }

    @Override
    public long getTtl(String key) {
        try {
            Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
            return ttl == null ? -1 : ttl;
        } catch (Exception exception) {
            LOGGER.warn("Error getting Redis cache TTL: {}", key, exception);
            return -1;
        }
    }
}
