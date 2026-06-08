package uk.gov.hmcts.appregister.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.appregister.testutils.BasePostgresIntegrationTest;

public class SecurityIntegrationTest extends BasePostgresIntegrationTest {

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private transient MockMvc mockMvc;

    @Test
    @DisplayName("Should block unauthenticated access to OpenAPI spec")
    void openApiSpec_shouldRequireAuthentication() throws Exception {
        mockMvc.perform(get("/specs/openapi.json")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should allow unauthenticated access to minimal /health")
    void healthEndpoint_shouldAllowAnonymousAccess() throws Exception {
        mockMvc.perform(get("/health")).andExpect(status().isOk());
    }

    @DisplayName("Should require authentication for OpenAPI documentation")
    @Test
    void openApiDocs_shouldRequireAuthentication() throws Exception {
        mockMvc.perform(get("/specs/openapi.json")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should block unauthenticated access to /rest-implementation-status")
    void restImplementationStatusEndpoint_shouldRequireAuthentication() throws Exception {
        mockMvc.perform(get("/rest-implementation-status")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return 401 for protected endpoint without JWT")
    void protectedEndpoint_shouldRequireAuthentication() throws Exception {
        mockMvc.perform(get("/some-protected-endpoint")).andExpect(status().isUnauthorized());
    }
}
