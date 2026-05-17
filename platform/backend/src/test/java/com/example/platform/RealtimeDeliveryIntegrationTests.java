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
class RealtimeDeliveryIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void pollingReturnsVersionedWorkspaceEventsSinceCursor() throws Exception {
        mockMvc.perform(post("/api/tenants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tenantName": "Realtime Corp",
                                  "workspaceName": "Realtime",
                                  "ownerUserId": "user-realtime-owner",
                                  "ownerEmail": "realtime-owner@example.com",
                                  "ownerDisplayName": "Realtime Owner"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workspaceId").value("workspace-realtime"));

        String loginPayload = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": "user-realtime-owner",
                                  "workspaceId": "workspace-realtime"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String token = JsonFieldExtractor.read(loginPayload, "token");

        mockMvc.perform(post("/api/workspaces/workspace-realtime/documents")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Realtime Delivery Notes",
                                  "content": "Polling, SSE, and WebSocket should share one versioned event shape"
                                }
                                """))
                .andExpect(status().isOk());

        String eventsPayload = mockMvc.perform(get("/api/v1/workspaces/workspace-realtime/events?since=0")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nextCursor").isNumber())
                .andExpect(jsonPath("$.events[?(@.eventType == 'DocumentCreatedEvent')]").isNotEmpty())
                .andExpect(jsonPath("$.events[0].schemaVersion").value("realtime.event.v1"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String nextCursor = JsonFieldExtractor.read(eventsPayload, "nextCursor");

        mockMvc.perform(get("/api/v1/workspaces/workspace-realtime/events?since=" + nextCursor)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nextCursor").isNumber())
                .andExpect(jsonPath("$.events").isEmpty());
    }
}
