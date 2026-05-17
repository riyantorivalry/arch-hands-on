package com.example.platform.common.infrastructure.ratelimit;

final class RateLimitInputs {

    private RateLimitInputs() {
    }

    static void validate(String key, int limit, int windowSeconds) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Rate limit key is required");
        }
        if (limit <= 0) {
            throw new IllegalArgumentException("Rate limit must be greater than zero");
        }
        if (windowSeconds <= 0) {
            throw new IllegalArgumentException("Rate limit windowSeconds must be greater than zero");
        }
    }

    static String cacheKey(String algorithm, String key) {
        return "ratelimit:" + algorithm + ":" + key.trim();
    }
}
