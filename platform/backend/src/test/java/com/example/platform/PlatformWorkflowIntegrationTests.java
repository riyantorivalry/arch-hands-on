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

        mockMvc.perform(post("/api/workspaces/workspace-engineering/memberships")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": "user-bob",
                                  "email": "bob@example.com",
                                  "displayName": "Bob",
                                  "role": "MEMBER"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("user-bob"))
                .andExpect(jsonPath("$.role").value("MEMBER"));

        String memberLoginPayload = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": "user-bob",
                                  "workspaceId": "workspace-engineering"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String memberToken = JsonFieldExtractor.read(memberLoginPayload, "token");

        mockMvc.perform(get("/api/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("user-alice"))
                .andExpect(jsonPath("$.tenantId").value("tenant-acme-corp"))
                .andExpect(jsonPath("$.role").value("OWNER"));

        mockMvc.perform(post("/api/tenants/tenant-acme-corp/workspaces")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "workspaceName": "Platform"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workspaceId").value("workspace-platform"))
                .andExpect(jsonPath("$.tenantId").value("tenant-acme-corp"));

        String platformLoginPayload = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": "user-alice",
                                  "workspaceId": "workspace-platform"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workspaceId").value("workspace-platform"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String platformToken = JsonFieldExtractor.read(platformLoginPayload, "token");

        mockMvc.perform(patch("/api/workspaces/workspace-platform/settings")
                        .header("Authorization", "Bearer " + platformToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "defaultDocumentStatus": "IN_REVIEW",
                                  "taskAutoAssignEnabled": false,
                                  "messageRetentionDays": 90
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workspaceId").value("workspace-platform"))
                .andExpect(jsonPath("$.defaultDocumentStatus").value("IN_REVIEW"))
                .andExpect(jsonPath("$.taskAutoAssignEnabled").value(false))
                .andExpect(jsonPath("$.messageRetentionDays").value(90));

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

        mockMvc.perform(post("/api/workspaces/workspace-engineering/channels")
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "restricted"
                                }
                                """))
                .andExpect(status().isForbidden());

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

        mockMvc.perform(get("/api/channels/channel-general/messages?page=0&size=1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].channelId").value("channel-general"));

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

        mockMvc.perform(get("/api/v1/workspaces/workspace-engineering/documents/search?query=architecture")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].documentId").value(persistedDocumentId))
                .andExpect(jsonPath("$[0].title").value("Architecture Notes"));

        mockMvc.perform(get("/api/v2/workspaces/workspace-engineering/documents/search?query=architecture")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].documentId").value(persistedDocumentId))
                .andExpect(jsonPath("$[0].title").value("Architecture Notes"));

        mockMvc.perform(get("/api/v3/workspaces/workspace-engineering/documents/search?query=architecture")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].documentId").value(persistedDocumentId))
                .andExpect(jsonPath("$[0].title").value("Architecture Notes"));

        mockMvc.perform(get("/api/workspaces/workspace-platform/documents")
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/documents/" + persistedDocumentId)
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Changed By Member",
                                  "content": "Not allowed"
                                }
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/documents/" + persistedDocumentId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Architecture Notes",
                                  "content": "Initial collaboration platform notes",
                                  "status": "IN_REVIEW"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_REVIEW"));

        mockMvc.perform(patch("/api/documents/" + persistedDocumentId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Architecture Notes",
                                  "content": "Initial collaboration platform notes",
                                  "status": "ACTIVE"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        mockMvc.perform(patch("/api/documents/" + persistedDocumentId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Architecture Notes",
                                  "content": "Changing a published document without returning it to draft",
                                  "status": "ACTIVE"
                                }
                                """))
                .andExpect(status().isConflict());

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

        String memberTaskPayload = mockMvc.perform(post("/api/workspaces/workspace-engineering/tasks")
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Member Task",
                                  "description": "Owned by member with completion notes",
                                  "assigneeUserId": "user-bob"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String memberTaskId = JsonFieldExtractor.read(memberTaskPayload, "taskId");

        mockMvc.perform(patch("/api/tasks/" + memberTaskId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Owner Moves Task",
                                  "description": "Owned by member with completion notes",
                                  "status": "IN_PROGRESS",
                                  "assigneeUserId": "user-bob"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        mockMvc.perform(patch("/api/tasks/" + memberTaskId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Owner Moves Task",
                                  "description": "Owned by member with completion notes",
                                  "status": "DONE",
                                  "assigneeUserId": "user-bob"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"));

        mockMvc.perform(patch("/api/tasks/" + taskId)
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Member Override",
                                  "description": "Not allowed",
                                  "status": "DONE",
                                  "assigneeUserId": "user-alice"
                                }
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }
}
