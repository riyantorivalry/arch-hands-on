package com.example.platform;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class CacheStrategyIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void caffeineStrategySupportsCacheSetGetAndIncrementThroughBenchmarkApi() throws Exception {
        mockMvc.perform(post("/api/tenants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tenantName": "Cache Corp",
                                  "workspaceName": "Cache",
                                  "ownerUserId": "user-cache-owner",
                                  "ownerEmail": "cache-owner@example.com",
                                  "ownerDisplayName": "Cache Owner"
                                }
                                """))
                .andExpect(status().isOk());

        String loginPayload = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": "user-cache-owner",
                                  "workspaceId": "workspace-cache"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String token = JsonFieldExtractor.read(loginPayload, "token");

        mockMvc.perform(get("/api/benchmarks/cache/strategy")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.selectedStrategy").value("caffeine"));

        mockMvc.perform(post("/api/benchmarks/cache/caffeine/entries")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "key": "cache-test-key",
                                  "value": {
                                    "message": "cached"
                                  },
                                  "ttlSeconds": 60
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.strategy").value("caffeine"))
                .andExpect(jsonPath("$.hit").value(true))
                .andExpect(jsonPath("$.value.message").value("cached"));

        mockMvc.perform(get("/api/benchmarks/cache/caffeine/entries/cache-test-key")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hit").value(true))
                .andExpect(jsonPath("$.value.message").value("cached"));

        mockMvc.perform(post("/api/benchmarks/cache/caffeine/counters/cache-counter-key/increment")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ttlSeconds": 60
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.value").value(1));
    }
}
