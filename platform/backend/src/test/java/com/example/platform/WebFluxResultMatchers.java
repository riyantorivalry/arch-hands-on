package com.example.platform;

import org.springframework.test.web.reactive.server.EntityExchangeResult;

final class WebFluxResultMatchers {

    private WebFluxResultMatchers() {
    }

    static WebFluxMockMvc.StatusAssertions status() {
        return new WebFluxMockMvc.StatusAssertions();
    }

    static WebFluxMockMvc.JsonAssertions jsonPath(String expression) {
        return new WebFluxMockMvc.JsonAssertions(expression);
    }

    @FunctionalInterface
    interface ResultMatcher {
        void match(EntityExchangeResult<String> result);
    }
}
