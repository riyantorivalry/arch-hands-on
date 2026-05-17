package com.example.platform.common.infrastructure.cache;

public interface CacheBackend {

    String name();

    Object get(String key);

    void set(String key, Object value, long ttlSeconds);

    void set(String key, Object value);

    void delete(String key);

    void deletePattern(String pattern);

    boolean exists(String key);

    long increment(String key, long ttlSeconds);

    long getTtl(String key);
}
