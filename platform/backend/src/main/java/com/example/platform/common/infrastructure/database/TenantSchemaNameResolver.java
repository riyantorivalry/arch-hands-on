package com.example.platform.common.infrastructure.database;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TenantSchemaNameResolver {

    private static final int POSTGRES_IDENTIFIER_LIMIT = 63;

    private final String prefix;

    public TenantSchemaNameResolver(@Value("${platform.tenancy.tenant-schema.prefix:tenant_}") String prefix) {
        this.prefix = sanitizePrefix(prefix);
    }

    public String schemaForTenant(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("tenantId is required");
        }
        String normalized = tenantId.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_]+", "_")
                .replaceAll("(^_+|_+$)", "");
        String schemaName = prefix + (normalized.isBlank() ? "unknown" : normalized);
        if (schemaName.length() <= POSTGRES_IDENTIFIER_LIMIT) {
            return schemaName;
        }
        String hash = shortHash(schemaName);
        return schemaName.substring(0, POSTGRES_IDENTIFIER_LIMIT - hash.length() - 1) + "_" + hash;
    }

    private String sanitizePrefix(String value) {
        String sanitized = (value == null || value.isBlank() ? "tenant_" : value)
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_]+", "_");
        if (!sanitized.endsWith("_")) {
            sanitized = sanitized + "_";
        }
        return sanitized;
    }

    private String shortHash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash, 0, 4);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 digest is not available", exception);
        }
    }
}
