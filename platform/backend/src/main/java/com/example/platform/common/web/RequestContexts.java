package com.example.platform.common.web;

public final class RequestContexts {

    private RequestContexts() {
    }

    public static RequestContext current() {
        return RequestContextHolder.get()
                .orElseThrow(() -> new IllegalStateException("Request context is not available"));
    }
}
