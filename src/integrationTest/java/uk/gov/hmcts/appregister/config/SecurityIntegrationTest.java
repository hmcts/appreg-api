package uk.gov.hmcts.appregister.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
public class SecurityIntegrationTest {

  @DynamicPropertySource
  static void registerPgProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.flyway.enabled", () -> "false");
  }

  @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
  @Autowired
  private transient MockMvc mockMvc;

  @Test
  @DisplayName("Should allow unauthenticated access to /health")
  void healthEndpoint_shouldAllowAnonymousAccess() throws Exception {
    MvcResult result = mockMvc.perform(get("/health")).andExpect(status().isOk()).andReturn();
  }

  @Test
  @DisplayName("Should allow unauthenticated access to /swagger-ui/index.html")
  void swaggerEndpoint_shouldAllowAnonymousAccess() throws Exception {
    mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
  }

  @Test
  @DisplayName("Should return 401 for protected endpoint without JWT")
  void protectedEndpoint_shouldRequireAuthentication() throws Exception {
    mockMvc.perform(get("/some-protected-endpoint")).andExpect(status().isUnauthorized());
  }
}
