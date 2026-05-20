package com.example.platform.common.web;

import com.example.platform.identityaccess.application.SessionAuthenticationService;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
public class TenantContextFilter implements WebFilter, Ordered {

    public static final String CORRELATION_HEADER = "X-Correlation-Id";
    private static final String AUTHORIZATION_HEADER = "Authorization";

    private final SessionAuthenticationService sessionAuthenticationService;

    public TenantContextFilter(SessionAuthenticationService sessionAuthenticationService) {
        this.sessionAuthenticationService = sessionAuthenticationService;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (shouldNotFilter(exchange)) {
            return chain.filter(exchange);
        }

        String correlationId = readHeader(exchange, CORRELATION_HEADER, UUID.randomUUID().toString());
        RequestContext context;
        try {
            context = resolveContext(exchange, correlationId);
        } catch (AuthenticationRequiredException exception) {
            return writeUnauthorized(exchange, exception);
        }

        exchange.getResponse().getHeaders().set(CORRELATION_HEADER, correlationId);
        RequestContextHolder.set(context);
        MDC.put("correlationId", context.correlationId());
        MDC.put("tenantId", valueOrDash(context.tenantId()));
        MDC.put("workspaceId", valueOrDash(context.workspaceId()));
        MDC.put("userId", valueOrDash(context.userId()));
        return chain.filter(exchange)
                .doFinally(signalType -> {
                    MDC.remove("correlationId");
                    MDC.remove("tenantId");
                    MDC.remove("workspaceId");
                    MDC.remove("userId");
                    RequestContextHolder.clear();
                });
    }

    private boolean shouldNotFilter(ServerWebExchange exchange) {
        String path = exchange.getRequest().getPath().pathWithinApplication().value();
        return path.startsWith("/actuator");
    }

    private String readHeader(ServerWebExchange exchange, String headerName, String fallback) {
        String value = exchange.getRequest().getHeaders().getFirst(headerName);
        return value == null || value.isBlank() ? fallback : value;
    }

    private RequestContext resolveContext(ServerWebExchange exchange, String correlationId) {
        String path = exchange.getRequest().getPath().pathWithinApplication().value();
        if (isPublicEndpoint(path)) {
            return new RequestContext(null, null, null, correlationId);
        }

        String authorization = exchange.getRequest().getHeaders().getFirst(AUTHORIZATION_HEADER);
        if (authorization == null || authorization.isBlank() || !authorization.startsWith("Bearer ")) {
            throw new AuthenticationRequiredException("Bearer token is required");
        }
        String token = authorization.substring("Bearer ".length()).trim();
        return sessionAuthenticationService.authenticate(token, correlationId);
    }

    private boolean isPublicEndpoint(String path) {
        return "/api/tenants".equals(path) || "/api/auth/login".equals(path);
    }

    private String valueOrDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private Mono<Void> writeUnauthorized(ServerWebExchange exchange, AuthenticationRequiredException exception) {
        var response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        byte[] body = ("{\"code\":\"UNAUTHORIZED\",\"message\":\"" + exception.getMessage() + "\"}")
                .getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(body);
        return response.writeWith(Mono.just(buffer));
    }
}
