package com.example.platform.common.web;

import reactor.core.publisher.Mono;

public final class RequestContexts {

    private RequestContexts() {
    }

    public static RequestContext current() {
        return RequestContextHolder.get()
                .orElseThrow(() -> new IllegalStateException("Request context is not available"));
    }

    public static RequestContext authenticated() {
        RequestContext context = current();
        if (!context.isAuthenticated()) {
            throw new AuthenticationRequiredException("Authentication is required");
        }
        return context;
    }

    public static Mono<RequestContext> currentReactive() {
        return Mono.deferContextual(contextView -> Mono.just(RequestContextHolder.get(contextView)
                .or(RequestContextHolder::get)
                .orElseThrow(() -> new IllegalStateException("Request context is not available"))));
    }

    public static Mono<RequestContext> authenticatedReactive() {
        return currentReactive().map(context -> {
            if (!context.isAuthenticated()) {
                throw new AuthenticationRequiredException("Authentication is required");
            }
            return context;
        });
    }
}
