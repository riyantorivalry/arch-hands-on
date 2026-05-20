package com.example.platform;

import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Objects;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

final class WebFluxMockMvc {

    private final WebTestClient webTestClient;

    WebFluxMockMvc(WebTestClient webTestClient) {
        this.webTestClient = webTestClient;
    }

    ResultActions perform(WebFluxRequestBuilders.RequestBuilder request) {
        WebTestClient.RequestBodySpec spec = webTestClient.method(request.method())
                .uri(request.uri())
                .headers(headers -> headers.addAll(request.headers()));
        if (request.contentType() != null) {
            spec.contentType(request.contentType());
        }
        WebTestClient.ResponseSpec responseSpec = request.content() == null
                ? spec.exchange()
                : spec.bodyValue(request.content()).exchange();
        return new ResultActions(responseSpec.expectBody(String.class).returnResult());
    }

    static final class ResultActions {
        private final EntityExchangeResult<String> result;

        private ResultActions(EntityExchangeResult<String> result) {
            this.result = result;
        }

        ResultActions andExpect(WebFluxResultMatchers.ResultMatcher matcher) {
            matcher.match(result);
            return this;
        }

        MvcResult andReturn() {
            return new MvcResult(result);
        }
    }

    static final class MvcResult {
        private final EntityExchangeResult<String> result;

        private MvcResult(EntityExchangeResult<String> result) {
            this.result = result;
        }

        MockHttpServletResponse getResponse() {
            return new MockHttpServletResponse(result);
        }
    }

    static final class MockHttpServletResponse {
        private final EntityExchangeResult<String> result;

        private MockHttpServletResponse(EntityExchangeResult<String> result) {
            this.result = result;
        }

        String getContentAsString() {
            String body = result.getResponseBody();
            if (body != null) {
                return body;
            }
            byte[] bytes = result.getResponseBodyContent();
            return bytes == null ? "" : new String(bytes, StandardCharsets.UTF_8);
        }
    }

    static final class JsonAssertions {
        private final String expression;

        JsonAssertions(String expression) {
            this.expression = expression;
        }

        WebFluxResultMatchers.ResultMatcher value(Object expected) {
            return result -> {
                Object actual = read(result);
                if (!valuesEqual(expected, actual)) {
                    throw new AssertionError("Expected JSON path " + expression
                            + " to be <" + expected + "> but was <" + actual + ">");
                }
            };
        }

        WebFluxResultMatchers.ResultMatcher isNumber() {
            return result -> {
                Object actual = read(result);
                if (!(actual instanceof Number)) {
                    throw new AssertionError("Expected JSON path " + expression + " to be a number but was <" + actual + ">");
                }
            };
        }

        WebFluxResultMatchers.ResultMatcher isEmpty() {
            return result -> {
                Object actual = read(result);
                if (actual instanceof Collection<?> collection && collection.isEmpty()) {
                    return;
                }
                throw new AssertionError("Expected JSON path " + expression + " to be empty but was <" + actual + ">");
            };
        }

        WebFluxResultMatchers.ResultMatcher isNotEmpty() {
            return result -> {
                Object actual = read(result);
                if (actual instanceof Collection<?> collection && !collection.isEmpty()) {
                    return;
                }
                throw new AssertionError("Expected JSON path " + expression + " to be non-empty but was <" + actual + ">");
            };
        }

        private Object read(EntityExchangeResult<String> result) {
            return JsonPath.read(body(result), expression);
        }

        private String body(EntityExchangeResult<String> result) {
            return Objects.requireNonNullElse(result.getResponseBody(), "");
        }

        private boolean valuesEqual(Object expected, Object actual) {
            if (expected instanceof Number expectedNumber && actual instanceof Number actualNumber) {
                return new BigDecimal(expectedNumber.toString()).compareTo(new BigDecimal(actualNumber.toString())) == 0;
            }
            return Objects.equals(expected, actual);
        }
    }

    static final class StatusAssertions {
        WebFluxResultMatchers.ResultMatcher isOk() {
            return status(HttpStatus.OK);
        }

        WebFluxResultMatchers.ResultMatcher isForbidden() {
            return status(HttpStatus.FORBIDDEN);
        }

        WebFluxResultMatchers.ResultMatcher isUnauthorized() {
            return status(HttpStatus.UNAUTHORIZED);
        }

        WebFluxResultMatchers.ResultMatcher isConflict() {
            return status(HttpStatus.CONFLICT);
        }

        private WebFluxResultMatchers.ResultMatcher status(HttpStatus expected) {
            return result -> {
                if (!Objects.equals(result.getStatus(), expected)) {
                    throw new AssertionError("Expected HTTP status " + expected + " but was " + result.getStatus());
                }
            };
        }
    }

    record Request(HttpMethod method, String uri, HttpHeaders headers, MediaType contentType, String content) {
    }
}
