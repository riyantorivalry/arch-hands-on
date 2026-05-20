package com.example.platform;

import static com.example.platform.WebFluxRequestBuilders.get;
import static com.example.platform.WebFluxRequestBuilders.post;
import static com.example.platform.WebFluxResultMatchers.jsonPath;
import static com.example.platform.WebFluxResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;

@SpringBootTest
class AnalyticsComparisonIntegrationTests extends WebFluxIntegrationTestSupport {

    @Test
    void postgresJsonbAnalyticsEndpointReturnsCapturedSearchEvents() throws Exception {
        mockMvc.perform(post("/api/tenants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tenantName": "Analytics Corp",
                                  "workspaceName": "Insights",
                                  "ownerUserId": "user-analytics-owner",
                                  "ownerEmail": "analytics-owner@example.com",
                                  "ownerDisplayName": "Analytics Owner"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenantId").value("tenant-analytics-corp"))
                .andExpect(jsonPath("$.workspaceId").value("workspace-insights"));

        String loginPayload = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": "user-analytics-owner",
                                  "workspaceId": "workspace-insights"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String token = JsonFieldExtractor.read(loginPayload, "token");

        mockMvc.perform(post("/api/workspaces/workspace-insights/documents")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Analytics JSONB Notes",
                                  "content": "Analytics events should be queryable from PostgreSQL JSONB"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/workspaces/workspace-insights/documents/search?query=jsonb")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/tenants/tenant-analytics-corp/analytics/events")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.eventType == 'search')]").isNotEmpty())
                .andExpect(jsonPath("$[?(@.eventData.query == 'jsonb')]").isNotEmpty());
    }
}
