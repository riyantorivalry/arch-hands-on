package com.example.platform.common.infrastructure;

import com.example.platform.common.infrastructure.cache.CacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Rate limiting service using Redis.
 * Phase 2: Protects APIs from abuse with configurable limits.
 */
@Component
public class RateLimitService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RateLimitService.class);

    private final CacheService cacheService;

    public RateLimitService(CacheService cacheService) {
        this.cacheService = cacheService;
    }

    /**
     * Check if request is allowed under rate limit.
     *
     * @param key Unique key for the rate limit (e.g., "user:123:api-calls")
     * @param limit Maximum requests allowed
     * @param windowSeconds Time window in seconds
     * @return true if request is allowed, false if rate limited
     */
    public boolean isAllowed(String key, int limit, int windowSeconds) {
        String counterKey = "ratelimit:" + key;

        // Get current count
        long currentCount = cacheService.increment(counterKey, windowSeconds);

        if (currentCount > limit) {
            LOGGER.warn("Rate limit exceeded for key: {} (count: {}, limit: {})", key, currentCount, limit);
            return false;
        }

        LOGGER.debug("Rate limit check passed for key: {} (count: {}, limit: {})", key, currentCount, limit);
        return true;
    }

    /**
     * Check rate limit for a user across all API calls.
     */
    public boolean isAllowedForUser(String userId, int limit, int windowSeconds) {
        return isAllowed("user:" + userId + ":global", limit, windowSeconds);
    }

    /**
     * Check rate limit for a specific API endpoint.
     */
    public boolean isAllowedForEndpoint(String userId, String endpoint, int limit, int windowSeconds) {
        return isAllowed("user:" + userId + ":endpoint:" + endpoint, limit, windowSeconds);
    }

    /**
     * Check rate limit for tenant-level operations.
     */
    public boolean isAllowedForTenant(String tenantId, String operation, int limit, int windowSeconds) {
        return isAllowed("tenant:" + tenantId + ":" + operation, limit, windowSeconds);
    }

    /**
     * Get remaining requests for a key.
     */
    public long getRemainingRequests(String key, int limit) {
        String counterKey = "ratelimit:" + key;
        long currentCount = cacheService.increment(counterKey, 60); // Just to get current value
        return Math.max(0, limit - (currentCount - 1)); // Subtract 1 because increment was called
    }

    /**
     * Get time until rate limit resets (in seconds).
     */
    public long getResetTime(String key) {
        String counterKey = "ratelimit:" + key;
        return cacheService.getTtl(counterKey);
    }
}
