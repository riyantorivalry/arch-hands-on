package com.example.platform.identityaccess.application;

import java.time.Instant;

public record TokenPair(
        String accessToken,
        String refreshToken,
        String tokenType,
        Instant accessTokenExpiresAt,
        Instant refreshTokenExpiresAt,
        long expiresIn
) {
}
