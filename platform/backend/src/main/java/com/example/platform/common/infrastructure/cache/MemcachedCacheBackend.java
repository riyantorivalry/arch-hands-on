package com.example.platform.common.infrastructure.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MemcachedCacheBackend implements CacheBackend {

    private static final Logger LOGGER = LoggerFactory.getLogger(MemcachedCacheBackend.class);

    private final ObjectMapper objectMapper;
    private final String host;
    private final int port;
    private final int timeoutMillis;

    public MemcachedCacheBackend(
            ObjectMapper objectMapper,
            @Value("${platform.feature.cache.memcached.host:localhost}") String host,
            @Value("${platform.feature.cache.memcached.port:11211}") int port,
            @Value("${platform.feature.cache.memcached.timeout-ms:1000}") int timeoutMillis
    ) {
        this.objectMapper = objectMapper;
        this.host = host;
        this.port = port;
        this.timeoutMillis = timeoutMillis;
    }

    @Override
    public String name() {
        return "memcached";
    }

    @Override
    public Object get(String key) {
        try {
            String safeKey = safeKey(key);
            return withConnection((reader, writer) -> {
                writer.write("get " + safeKey + "\r\n");
                writer.flush();
                String header = reader.readLine();
                if (header == null || header.equals("END")) {
                    return null;
                }
                String[] parts = header.split(" ");
                int bytes = Integer.parseInt(parts[3]);
                char[] payload = new char[bytes];
                int read = reader.read(payload);
                reader.readLine();
                reader.readLine();
                if (read <= 0) {
                    return null;
                }
                byte[] decoded = Base64.getDecoder().decode(new String(payload, 0, read));
                return objectMapper.readValue(decoded, Object.class);
            });
        } catch (Exception exception) {
            LOGGER.warn("Error getting Memcached key: {}", key, exception);
            return null;
        }
    }

    @Override
    public void set(String key, Object value, long ttlSeconds) {
        try {
            String safeKey = safeKey(key);
            byte[] json = objectMapper.writeValueAsBytes(value);
            String payload = Base64.getEncoder().encodeToString(json);
            withConnection((reader, writer) -> {
                writer.write("set " + safeKey + " 0 " + Math.max(0, ttlSeconds) + " " + payload.length() + "\r\n");
                writer.write(payload + "\r\n");
                writer.flush();
                reader.readLine();
                return null;
            });
        } catch (Exception exception) {
            LOGGER.warn("Error setting Memcached key: {}", key, exception);
        }
    }

    @Override
    public void set(String key, Object value) {
        set(key, value, 0);
    }

    @Override
    public void delete(String key) {
        try {
            String safeKey = safeKey(key);
            withConnection((reader, writer) -> {
                writer.write("delete " + safeKey + "\r\n");
                writer.flush();
                reader.readLine();
                return null;
            });
        } catch (Exception exception) {
            LOGGER.warn("Error deleting Memcached key: {}", key, exception);
        }
    }

    @Override
    public void deletePattern(String pattern) {
        if (!pattern.contains("*")) {
            delete(pattern);
        }
    }

    @Override
    public boolean exists(String key) {
        return get(key) != null;
    }

    @Override
    public long increment(String key, long ttlSeconds) {
        try {
            String safeKey = safeKey(key);
            return withConnection((reader, writer) -> {
                writer.write("incr " + safeKey + " 1\r\n");
                writer.flush();
                String response = reader.readLine();
                if (response != null && !response.equals("NOT_FOUND")) {
                    return Long.parseLong(response);
                }
                writer.write("add " + safeKey + " 0 " + Math.max(0, ttlSeconds) + " 1\r\n1\r\n");
                writer.flush();
                reader.readLine();
                return 1L;
            });
        } catch (Exception exception) {
            LOGGER.warn("Error incrementing Memcached key: {}", key, exception);
            return 0;
        }
    }

    @Override
    public long getTtl(String key) {
        return -1;
    }

    private String safeKey(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Cache key is required");
        }
        String safeKey = key.trim();
        if (safeKey.length() > 250 || safeKey.matches(".*\\s+.*")) {
            throw new IllegalArgumentException("Memcached keys must be <= 250 characters and contain no whitespace");
        }
        return safeKey;
    }

    private <T> T withConnection(MemcachedOperation<T> operation) throws Exception {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeoutMillis);
            socket.setSoTimeout(timeoutMillis);
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
            return operation.execute(reader, writer);
        }
    }

    @FunctionalInterface
    private interface MemcachedOperation<T> {
        T execute(BufferedReader reader, BufferedWriter writer) throws Exception;
    }
}
