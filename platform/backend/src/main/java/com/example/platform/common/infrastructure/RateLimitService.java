package com.example.platform.common.infrastructure;

import com.example.platform.common.infrastructure.ratelimit.RateLimitAlgorithm;
import com.example.platform.common.infrastructure.ratelimit.RateLimitDecision;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Rate limiting service backed by a configurable algorithm.
 */
@Component
public class RateLimitService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RateLimitService.class);

    private final List<RateLimitAlgorithm> algorithms;
    private final String mode;

    public RateLimitService(
            List<RateLimitAlgorithm> algorithms,
            @Value("${platform.feature.rate-limit.mode:fixed-window}") String mode
    ) {
        this.algorithms = algorithms;
        this.mode = normalize(mode);
    }

    public String mode() {
        return mode;
    }

    public Mono<RateLimitDecision> check(String key, int limit, int windowSeconds) {
        return selected().check(key, limit, windowSeconds);
    }

    public Mono<RateLimitDecision> check(String algorithm, String key, int limit, int windowSeconds) {
        return algorithm(algorithm).check(key, limit, windowSeconds);
    }

    public RateLimitAlgorithm algorithm(String algorithm) {
        String normalized = normalize(algorithm);
        return algorithms.stream()
                .filter(candidate -> candidate.name().equals(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported rate limit algorithm: " + algorithm));
    }

    /**
     * Check if request is allowed under rate limit.
     *
     * @param key Unique key for the rate limit (e.g., "user:123:api-calls")
     * @param limit Maximum requests allowed
     * @param windowSeconds Time window in seconds
     * @return true if request is allowed, false if rate limited
     */
    public Mono<Boolean> isAllowed(String key, int limit, int windowSeconds) {
        return check(key, limit, windowSeconds)
                .map(decision -> {
                    if (!decision.allowed()) {
                        LOGGER.warn("Rate limit exceeded for key: {} (algorithm: {}, limit: {}, retryAfterSeconds: {})",
                                key, decision.algorithm(), decision.limit(), decision.retryAfterSeconds());
                        return false;
                    }

                    LOGGER.debug("Rate limit check passed for key: {} (algorithm: {}, remaining: {}, limit: {})",
                            key, decision.algorithm(), decision.remaining(), decision.limit());
                    return true;
                });
    }

    /**
     * Check rate limit for a user across all API calls.
     */
    public Mono<Boolean> isAllowedForUser(String userId, int limit, int windowSeconds) {
        return isAllowed("user:" + userId + ":global", limit, windowSeconds);
    }

    /**
     * Check rate limit for a specific API endpoint.
     */
    public Mono<Boolean> isAllowedForEndpoint(String userId, String endpoint, int limit, int windowSeconds) {
        return isAllowed("user:" + userId + ":endpoint:" + endpoint, limit, windowSeconds);
    }

    /**
     * Check rate limit for tenant-level operations.
     */
    public Mono<Boolean> isAllowedForTenant(String tenantId, String operation, int limit, int windowSeconds) {
        return isAllowed("tenant:" + tenantId + ":" + operation, limit, windowSeconds);
    }

    /**
     * Get remaining requests for a key.
     */
    public Mono<Long> getRemainingRequests(String key, int limit) {
        return check(key, limit, 60).map(RateLimitDecision::remaining);
    }

    /**
     * Get time until rate limit resets (in seconds).
     */
    public Mono<Long> getResetTime(String key) {
        return check(key, Integer.MAX_VALUE, 60).map(RateLimitDecision::resetSeconds);
    }

    private RateLimitAlgorithm selected() {
        return algorithm(mode);
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "fixed-window";
        }
        return value.trim().toLowerCase();
    }
}
