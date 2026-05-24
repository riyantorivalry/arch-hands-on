package com.example.platform.identityaccess.infrastructure.security;

import com.example.platform.common.web.AuthenticationRequiredException;
import com.example.platform.identityaccess.domain.UserSessionEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;

@Service
public class JwtAccessTokenService {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final Base64.Encoder BASE64_URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder BASE64_URL_DECODER = Base64.getUrlDecoder();

    private final JwtAuthenticationProperties properties;
    private final ObjectMapper objectMapper;

    public JwtAccessTokenService(JwtAuthenticationProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public IssuedAccessToken issue(UserSessionEntity session, Instant issuedAt) {
        Instant expiresAt = issuedAt.plus(properties.getAccessTokenTtl());
        Map<String, Object> header = new LinkedHashMap<>();
        header.put("alg", "HS256");
        header.put("typ", "JWT");

        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("iss", properties.getIssuer());
        claims.put("aud", properties.getAudience());
        claims.put("sub", session.getUserId());
        claims.put("sid", session.getSessionToken());
        claims.put("tid", session.getTenantId());
        claims.put("wid", session.getWorkspaceId());
        claims.put("cid", session.getClientId());
        claims.put("cty", session.getClientType());
        claims.put("typ", "access");
        claims.put("iat", issuedAt.getEpochSecond());
        claims.put("nbf", issuedAt.getEpochSecond());
        claims.put("exp", expiresAt.getEpochSecond());
        claims.put("jti", UUID.randomUUID().toString());

        return new IssuedAccessToken(sign(header, claims), expiresAt, properties.getAccessTokenTtl().toSeconds());
    }

    public AccessTokenClaims verify(String token) {
        String[] parts = token == null ? new String[0] : token.split("\\.");
        if (parts.length != 3) {
            throw new AuthenticationRequiredException("Invalid access token");
        }
        Map<String, Object> header = readJson(parts[0]);
        requireClaim(header, "alg", "HS256");
        requireClaim(header, "typ", "JWT");

        String expectedSignature = sign(parts[0] + "." + parts[1]);
        if (!MessageDigest.isEqual(expectedSignature.getBytes(StandardCharsets.US_ASCII), parts[2].getBytes(StandardCharsets.US_ASCII))) {
            throw new AuthenticationRequiredException("Invalid access token signature");
        }

        Map<String, Object> claims = readJson(parts[1]);
        requireClaim(claims, "iss", properties.getIssuer());
        requireClaim(claims, "aud", properties.getAudience());
        requireClaim(claims, "typ", "access");

        Instant now = Instant.now();
        Instant expiresAt = readInstant(claims, "exp");
        Instant notBefore = readInstant(claims, "nbf");
        if (expiresAt.plus(properties.getClockSkew()).isBefore(now)) {
            throw new AuthenticationRequiredException("Access token is expired");
        }
        if (notBefore.minus(properties.getClockSkew()).isAfter(now)) {
            throw new AuthenticationRequiredException("Access token is not valid yet");
        }

        return new AccessTokenClaims(
                readString(claims, "sid"),
                readString(claims, "sub"),
                readString(claims, "tid"),
                readString(claims, "wid"),
                readString(claims, "cid"),
                readString(claims, "cty"),
                expiresAt
        );
    }

    private String sign(Map<String, Object> header, Map<String, Object> claims) {
        String unsigned = encodeJson(header) + "." + encodeJson(claims);
        return unsigned + "." + sign(unsigned);
    }

    private String sign(String unsignedToken) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(properties.getSecret().getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return BASE64_URL_ENCODER.encodeToString(mac.doFinal(unsignedToken.getBytes(StandardCharsets.US_ASCII)));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to sign access token", exception);
        }
    }

    private String encodeJson(Map<String, Object> value) {
        try {
            return BASE64_URL_ENCODER.encodeToString(objectMapper.writeValueAsBytes(value));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to encode access token", exception);
        }
    }

    private Map<String, Object> readJson(String encoded) {
        try {
            return objectMapper.readValue(BASE64_URL_DECODER.decode(encoded), MAP_TYPE);
        } catch (Exception exception) {
            throw new AuthenticationRequiredException("Invalid access token payload");
        }
    }

    private void requireClaim(Map<String, Object> claims, String claimName, String expected) {
        String actual = readString(claims, claimName);
        if (!expected.equals(actual)) {
            throw new AuthenticationRequiredException("Invalid access token " + claimName);
        }
    }

    private String readString(Map<String, Object> claims, String claimName) {
        Object value = claims.get(claimName);
        if (!(value instanceof String stringValue) || stringValue.isBlank()) {
            throw new AuthenticationRequiredException("Access token is missing " + claimName);
        }
        return stringValue;
    }

    private Instant readInstant(Map<String, Object> claims, String claimName) {
        Object value = claims.get(claimName);
        if (value instanceof Number number) {
            return Instant.ofEpochSecond(number.longValue());
        }
        throw new AuthenticationRequiredException("Access token is missing " + claimName);
    }

    public record IssuedAccessToken(String token, Instant expiresAt, long expiresIn) {
    }

    public record AccessTokenClaims(
            String sessionId,
            String userId,
            String tenantId,
            String workspaceId,
            String clientId,
            String clientType,
            Instant expiresAt
    ) {
    }
}
