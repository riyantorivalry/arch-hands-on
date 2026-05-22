package com.example.platform.common.web;

import java.util.Optional;
import reactor.util.context.ContextView;

public final class RequestContextHolder {

    public static final Class<RequestContext> REACTOR_CONTEXT_KEY = RequestContext.class;

    private static final ThreadLocal<RequestContext> CONTEXT = new ThreadLocal<>();

    private RequestContextHolder() {
    }

    public static void set(RequestContext context) {
        CONTEXT.set(context);
    }

    public static Optional<RequestContext> get() {
        return Optional.ofNullable(CONTEXT.get());
    }

    public static Optional<RequestContext> get(ContextView contextView) {
        return contextView.hasKey(REACTOR_CONTEXT_KEY)
                ? Optional.of(contextView.get(REACTOR_CONTEXT_KEY))
                : Optional.empty();
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
