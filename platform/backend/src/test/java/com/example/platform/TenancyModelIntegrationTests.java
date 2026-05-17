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
class TenancyModelIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void sharedSchemaIsTheDefaultTenancyModel() throws Exception {
        mockMvc.perform(post("/api/tenants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tenantName": "Schema Corp",
                                  "workspaceName": "Schema",
                                  "ownerUserId": "user-schema-owner",
                                  "ownerEmail": "schema-owner@example.com",
                                  "ownerDisplayName": "Schema Owner"
                                }
                                """))
                .andExpect(status().isOk());

        String loginPayload = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": "user-schema-owner",
                                  "workspaceId": "workspace-schema"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String token = JsonFieldExtractor.read(loginPayload, "token");

        mockMvc.perform(get("/api/benchmarks/tenancy/model")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode").value("shared-schema"))
                .andExpect(jsonPath("$.defaultSchema").value("public"))
                .andExpect(jsonPath("$.tenantSchemaRoutingEnabled").value(false));

        mockMvc.perform(get("/api/benchmarks/tenancy/tenants/tenant-schema-corp/schema")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenantId").value("tenant-schema-corp"))
                .andExpect(jsonPath("$.schemaName").value("tenant_tenant_schema_corp"))
                .andExpect(jsonPath("$.provisionedWhenTenantCreated").value(false));
    }
}
