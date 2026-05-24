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

@SpringBootTest(properties = "platform.tenancy.mode=tenant-schema")
@AutoConfigureMockMvc
class TenantSchemaModeIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void tenantSchemaModeStartsAndProvisionsTenantSchema() throws Exception {
        mockMvc.perform(post("/api/tenants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tenantName": "Tenant Schema Mode Corp",
                                  "workspaceName": "Tenant Schema Mode",
                                  "ownerUserId": "user-tenant-schema-mode-owner",
                                  "ownerEmail": "tenant-schema-mode-owner@example.com",
                                  "ownerDisplayName": "Tenant Schema Mode Owner"
                                }
                                """))
                .andExpect(status().isOk());

        String loginPayload = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": "user-tenant-schema-mode-owner",
                                  "workspaceId": "workspace-tenant-schema-mode"
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
                .andExpect(jsonPath("$.mode").value("tenant-schema"))
                .andExpect(jsonPath("$.defaultSchema").value("public"))
                .andExpect(jsonPath("$.tenantSchemaRoutingEnabled").value(true));

        mockMvc.perform(get("/api/benchmarks/tenancy/tenants/tenant-tenant-schema-mode-corp/schema")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenantId").value("tenant-tenant-schema-mode-corp"))
                .andExpect(jsonPath("$.schemaName").value("tenant_tenant_tenant_schema_mode_corp"))
                .andExpect(jsonPath("$.provisionedWhenTenantCreated").value(true));
    }
}
