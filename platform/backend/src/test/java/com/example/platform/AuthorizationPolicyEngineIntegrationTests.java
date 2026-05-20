package com.example.platform;

import static com.example.platform.WebFluxRequestBuilders.get;
import static com.example.platform.WebFluxRequestBuilders.post;
import static com.example.platform.WebFluxResultMatchers.jsonPath;
import static com.example.platform.WebFluxResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;

@SpringBootTest
class AuthorizationPolicyEngineIntegrationTests extends WebFluxIntegrationTestSupport {

    @Test
    void selectedAndBenchmarkPolicyEnginesEvaluateTheSameRequestShape() throws Exception {
        mockMvc.perform(post("/api/tenants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tenantName": "Engine Corp",
                                  "workspaceName": "Engine",
                                  "ownerUserId": "user-engine-owner",
                                  "ownerEmail": "engine-owner@example.com",
                                  "ownerDisplayName": "Engine Owner"
                                }
                                """))
                .andExpect(status().isOk());

        String ownerLoginPayload = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": "user-engine-owner",
                                  "workspaceId": "workspace-engine"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String ownerToken = JsonFieldExtractor.read(ownerLoginPayload, "token");

        mockMvc.perform(post("/api/workspaces/workspace-engine/memberships")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": "user-engine-member",
                                  "email": "engine-member@example.com",
                                  "displayName": "Engine Member",
                                  "role": "MEMBER"
                                }
                                """))
                .andExpect(status().isOk());

        String memberLoginPayload = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": "user-engine-member",
                                  "workspaceId": "workspace-engine"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String memberToken = JsonFieldExtractor.read(memberLoginPayload, "token");

        mockMvc.perform(get("/api/benchmarks/authorization/engine")
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.selectedEngine").value("in-code"));

        String documentUpdateDecision = """
                {
                  "action": "document:update",
                  "resourceType": "document",
                  "resourceId": "document-owned-by-member",
                  "resourceTenantId": "tenant-engine-corp",
                  "resourceOwnerUserId": "user-engine-member",
                  "attributes": {
                    "status": "DRAFT",
                    "riskLevel": "LOW"
                  }
                }
                """;

        mockMvc.perform(post("/api/workspaces/workspace-engine/authorization/decisions")
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(documentUpdateDecision))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.strategy").value("in-code"))
                .andExpect(jsonPath("$.allowed").value(true))
                .andExpect(jsonPath("$.matchedRules[0]").value("allow.resource_owner"));

        assertEngineAllows(memberToken, "in-code", documentUpdateDecision);
        assertEngineAllows(memberToken, "opa-local", documentUpdateDecision);
        assertEngineAllows(memberToken, "casbin-local", documentUpdateDecision);
        assertEngineAllows(memberToken, "db-policy", documentUpdateDecision);
    }

    private void assertEngineAllows(String token, String engine, String request) throws Exception {
        mockMvc.perform(post("/api/benchmarks/authorization/" + engine + "/decisions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.strategy").value(engine))
                .andExpect(jsonPath("$.allowed").value(true));
    }
}
