package com.example.platform;

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
class AuthorizationComparisonIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void comparesRbacAndAbacOpaAuthorizationDecisions() throws Exception {
        mockMvc.perform(post("/api/tenants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tenantName": "Policy Corp",
                                  "workspaceName": "Policy",
                                  "ownerUserId": "user-policy-owner",
                                  "ownerEmail": "policy-owner@example.com",
                                  "ownerDisplayName": "Policy Owner"
                                }
                                """))
                .andExpect(status().isOk());

        String ownerLoginPayload = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": "user-policy-owner",
                                  "workspaceId": "workspace-policy"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String ownerToken = JsonFieldExtractor.read(ownerLoginPayload, "token");

        mockMvc.perform(post("/api/workspaces/workspace-policy/memberships")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": "user-policy-member",
                                  "email": "policy-member@example.com",
                                  "displayName": "Policy Member",
                                  "role": "MEMBER"
                                }
                                """))
                .andExpect(status().isOk());

        String memberLoginPayload = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": "user-policy-member",
                                  "workspaceId": "workspace-policy"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String memberToken = JsonFieldExtractor.read(memberLoginPayload, "token");

        String documentUpdateDecision = """
                {
                  "action": "document:update",
                  "resourceType": "document",
                  "resourceId": "document-owned-by-member",
                  "resourceTenantId": "tenant-policy-corp",
                  "resourceOwnerUserId": "user-policy-member",
                  "attributes": {
                    "status": "DRAFT",
                    "riskLevel": "LOW"
                  }
                }
                """;

        mockMvc.perform(post("/api/v1/workspaces/workspace-policy/authorization/decisions")
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(documentUpdateDecision))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.strategyVersion").value("v1"))
                .andExpect(jsonPath("$.strategy").value("rbac"))
                .andExpect(jsonPath("$.allowed").value(false));

        mockMvc.perform(post("/api/v2/workspaces/workspace-policy/authorization/decisions")
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(documentUpdateDecision))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.strategyVersion").value("v2"))
                .andExpect(jsonPath("$.strategy").value("abac-opa"))
                .andExpect(jsonPath("$.allowed").value(true))
                .andExpect(jsonPath("$.matchedRules[0]").value("allow.resource_owner_document_update"));
    }
}
