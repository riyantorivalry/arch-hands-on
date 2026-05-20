package com.example.platform;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

final class WebFluxRequestBuilders {

    private WebFluxRequestBuilders() {
    }

    static RequestBuilder get(String uri) {
        return new RequestBuilder(HttpMethod.GET, uri);
    }

    static RequestBuilder patch(String uri) {
        return new RequestBuilder(HttpMethod.PATCH, uri);
    }

    static RequestBuilder post(String uri) {
        return new RequestBuilder(HttpMethod.POST, uri);
    }

    static final class RequestBuilder {
        private final HttpMethod method;
        private final String uri;
        private final HttpHeaders headers = new HttpHeaders();
        private MediaType contentType;
        private String content;

        private RequestBuilder(HttpMethod method, String uri) {
            this.method = method;
            this.uri = uri;
        }

        RequestBuilder header(String name, String value) {
            headers.add(name, value);
            return this;
        }

        RequestBuilder contentType(MediaType contentType) {
            this.contentType = contentType;
            return this;
        }

        RequestBuilder content(String content) {
            this.content = content;
            return this;
        }

        HttpMethod method() {
            return method;
        }

        String uri() {
            return uri;
        }

        HttpHeaders headers() {
            return headers;
        }

        MediaType contentType() {
            return contentType;
        }

        String content() {
            return content;
        }
    }
}
