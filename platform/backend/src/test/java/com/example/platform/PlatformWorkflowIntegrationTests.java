package com.example.platform;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
class PlatformWorkflowIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createsTenantBootstrapsWorkspaceMembershipAndPostsMessage() throws Exception {
        mockMvc.perform(post("/api/tenants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tenantName": "Acme Corp",
                                  "workspaceName": "Engineering",
                                  "ownerUserId": "user-alice",
                                  "ownerEmail": "alice@example.com",
                                  "ownerDisplayName": "Alice"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenantId").value("tenant-acme-corp"))
                .andExpect(jsonPath("$.workspaceId").value("workspace-engineering"));

        String loginPayload = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": "user-alice",
                                  "workspaceId": "workspace-engineering"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("user-alice"))
                .andExpect(jsonPath("$.workspaceId").value("workspace-engineering"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String token = JsonFieldExtractor.read(loginPayload, "token");

        mockMvc.perform(get("/api/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("user-alice"))
                .andExpect(jsonPath("$.tenantId").value("tenant-acme-corp"))
                .andExpect(jsonPath("$.role").value("OWNER"));

        mockMvc.perform(post("/api/workspaces/workspace-engineering/channels")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "general"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.channelId").value("channel-general"));

        mockMvc.perform(post("/api/channels/channel-general/messages")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "body": "Hello team"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authorUserId").value("user-alice"))
                .andExpect(jsonPath("$.body").value("Hello team"));

        mockMvc.perform(get("/api/channels/channel-general/messages")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].channelId").value("channel-general"))
                .andExpect(jsonPath("$[0].body").value("Hello team"));

        String documentId = mockMvc.perform(post("/api/workspaces/workspace-engineering/documents")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Architecture Notes",
                                  "content": "Initial collaboration platform notes"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workspaceId").value("workspace-engineering"))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String persistedDocumentId = JsonFieldExtractor.read(documentId, "documentId");

        mockMvc.perform(post("/api/documents/" + persistedDocumentId + "/comments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "body": "Document comment"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentId").value(persistedDocumentId))
                .andExpect(jsonPath("$.body").value("Document comment"));

        mockMvc.perform(get("/api/workspaces/workspace-engineering/documents")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].documentId").value(persistedDocumentId))
                .andExpect(jsonPath("$[0].title").value("Architecture Notes"));

        String taskPayload = mockMvc.perform(post("/api/workspaces/workspace-engineering/tasks")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Bootstrap API",
                                  "description": "Implement the first API slice",
                                  "assigneeUserId": "user-alice"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workspaceId").value("workspace-engineering"))
                .andExpect(jsonPath("$.status").value("TODO"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String taskId = JsonFieldExtractor.read(taskPayload, "taskId");

        mockMvc.perform(patch("/api/tasks/" + taskId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Bootstrap API",
                                  "description": "Implement the first API slice",
                                  "status": "IN_PROGRESS",
                                  "assigneeUserId": "user-alice"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskId").value(taskId))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        mockMvc.perform(post("/api/tasks/" + taskId + "/comments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "body": "Task comment"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskId").value(taskId))
                .andExpect(jsonPath("$.body").value("Task comment"));

        mockMvc.perform(get("/api/workspaces/workspace-engineering/tasks")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].taskId").value(taskId))
                .andExpect(jsonPath("$[0].status").value("IN_PROGRESS"));

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }
}
