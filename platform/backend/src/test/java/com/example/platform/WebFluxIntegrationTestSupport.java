package com.example.platform;

import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.test.web.reactive.server.WebTestClient;

@AutoConfigureWebTestClient
abstract class WebFluxIntegrationTestSupport {

    @Autowired
    private WebTestClient webTestClient;

    protected WebFluxMockMvc mockMvc;

    @BeforeEach
    void setUpWebFluxMockMvc() {
        mockMvc = new WebFluxMockMvc(webTestClient.mutate()
                .responseTimeout(Duration.ofSeconds(30))
                .build());
    }
}
