package com.example.platform.common.web;

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
}
