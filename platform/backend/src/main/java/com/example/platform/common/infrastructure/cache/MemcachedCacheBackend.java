package com.example.platform.common.infrastructure.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.netty.tcp.TcpClient;

@Component
@ConditionalOnProperty(name = "platform.feature.cache.memcached.enabled", havingValue = "true")
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
    public Mono<Object> get(String key) {
        String safeKey = safeKey(key);
        return exchange("get " + safeKey + "\r\n", "END\r\n")
                .flatMap(response -> {
                    String[] lines = response.split("\r\n");
                    if (lines.length < 3 || lines[0].equals("END")) {
                        return Mono.empty();
                    }
                    try {
                        byte[] decoded = Base64.getDecoder().decode(lines[1]);
                        return Mono.just(objectMapper.readValue(decoded, Object.class));
                    } catch (Exception exception) {
                        return Mono.error(exception);
                    }
                })
                .doOnError(exception -> LOGGER.warn("Error getting Memcached key: {}", key, exception))
                .onErrorResume(exception -> Mono.empty());
    }

    @Override
    public Mono<Void> set(String key, Object value, long ttlSeconds) {
        String safeKey = safeKey(key);
        return Mono.fromCallable(() -> {
                    byte[] json = objectMapper.writeValueAsBytes(value);
                    String payload = Base64.getEncoder().encodeToString(json);
                    return "set " + safeKey + " 0 " + Math.max(0, ttlSeconds) + " " + payload.length()
                            + "\r\n" + payload + "\r\n";
                })
                .flatMap(command -> exchange(command, "STORED\r\n"))
                .doOnError(exception -> LOGGER.warn("Error setting Memcached key: {}", key, exception))
                .onErrorResume(exception -> Mono.empty())
                .then();
    }

    @Override
    public Mono<Void> set(String key, Object value) {
        return set(key, value, 0);
    }

    @Override
    public Mono<Void> delete(String key) {
        String safeKey = safeKey(key);
        return exchange("delete " + safeKey + "\r\n", "\r\n")
                .doOnError(exception -> LOGGER.warn("Error deleting Memcached key: {}", key, exception))
                .onErrorResume(exception -> Mono.empty())
                .then();
    }

    @Override
    public Mono<Void> deletePattern(String pattern) {
        if (!pattern.contains("*")) {
            return delete(pattern);
        }
        return Mono.empty();
    }

    @Override
    public Mono<Boolean> exists(String key) {
        return get(key).map(value -> true).defaultIfEmpty(false);
    }

    @Override
    public Mono<Long> increment(String key, long ttlSeconds) {
        String safeKey = safeKey(key);
        return exchange("incr " + safeKey + " 1\r\n", "\r\n")
                .flatMap(response -> {
                    String firstLine = response.split("\r\n", 2)[0];
                    if (!"NOT_FOUND".equals(firstLine)) {
                        return Mono.just(Long.parseLong(firstLine));
                    }
                    return exchange("add " + safeKey + " 0 " + Math.max(0, ttlSeconds) + " 1\r\n1\r\n", "\r\n")
                            .thenReturn(1L);
                })
                .doOnError(exception -> LOGGER.warn("Error incrementing Memcached key: {}", key, exception))
                .onErrorReturn(0L);
    }

    @Override
    public Mono<Long> getTtl(String key) {
        return Mono.just(-1L);
    }

    private Mono<String> exchange(String command, String terminator) {
        return TcpClient.create()
                .host(host)
                .port(port)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, timeoutMillis)
                .doOnConnected(connection -> connection.addHandlerLast(new ReadTimeoutHandler(timeoutMillis, TimeUnit.MILLISECONDS)))
                .connect()
                .flatMap(connection -> connection.outbound()
                        .sendString(Mono.just(command), StandardCharsets.UTF_8)
                        .then()
                        .thenMany(connection.inbound().receive().asString(StandardCharsets.UTF_8))
                        .scan("", String::concat)
                        .filter(response -> response.endsWith(terminator) || response.contains(terminator))
                        .next()
                        .doFinally(signalType -> connection.dispose()));
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
}
