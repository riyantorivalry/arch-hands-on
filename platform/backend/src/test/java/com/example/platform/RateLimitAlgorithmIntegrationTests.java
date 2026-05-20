package com.example.platform;

import static com.example.platform.WebFluxRequestBuilders.get;
import static com.example.platform.WebFluxRequestBuilders.post;
import static com.example.platform.WebFluxResultMatchers.jsonPath;
import static com.example.platform.WebFluxResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;

@SpringBootTest
class RateLimitAlgorithmIntegrationTests extends WebFluxIntegrationTestSupport {

    @Test
    void rateLimitAlgorithmsExposeBenchmarkDecisions() throws Exception {
        mockMvc.perform(post("/api/tenants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tenantName": "Rate Limit Corp",
                                  "workspaceName": "Rate Limit",
                                  "ownerUserId": "user-rate-limit-owner",
                                  "ownerEmail": "rate-limit-owner@example.com",
                                  "ownerDisplayName": "Rate Limit Owner"
                                }
                                """))
                .andExpect(status().isOk());

        String loginPayload = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": "user-rate-limit-owner",
                                  "workspaceId": "workspace-rate-limit"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String token = JsonFieldExtractor.read(loginPayload, "token");

        mockMvc.perform(get("/api/benchmarks/rate-limit/algorithm")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.selectedAlgorithm").value("fixed-window"));

        assertLimitThenRejects(token, "fixed-window", 2);
        assertLimitThenRejects(token, "sliding-window", 1);
        assertLimitThenRejects(token, "token-bucket", 2);
    }

    private void assertLimitThenRejects(String token, String algorithm, int limit) throws Exception {
        String key = algorithm + "-" + UUID.randomUUID();
        for (int attempt = 1; attempt <= limit; attempt++) {
            mockMvc.perform(post("/api/benchmarks/rate-limit/" + algorithm + "/decisions")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(request(key, limit)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.algorithm").value(algorithm))
                    .andExpect(jsonPath("$.key").value(key))
                    .andExpect(jsonPath("$.allowed").value(true));
        }

        mockMvc.perform(post("/api/benchmarks/rate-limit/" + algorithm + "/decisions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request(key, limit)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.algorithm").value(algorithm))
                .andExpect(jsonPath("$.allowed").value(false))
                .andExpect(jsonPath("$.remaining").value(0));
    }

    private String request(String key, int limit) {
        return """
                {
                  "key": "%s",
                  "limit": %d,
                  "windowSeconds": 60
                }
                """.formatted(key, limit);
    }
}
