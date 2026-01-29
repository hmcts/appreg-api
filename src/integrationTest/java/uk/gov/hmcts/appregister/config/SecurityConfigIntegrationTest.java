package uk.gov.hmcts.appregister.config;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.appregister.testutils.BaseIntegration;

@Import({
    SecurityConfigIntegrationTest.UserRoleController.class,
    SecurityConfigIntegrationTest.AdminRoleController.class
})
@TestPropertySource(properties = {"azure-tenant-id=dummy-tenant-id"})
class SecurityConfigIntegrationTest extends BaseIntegration {

    @Autowired MockMvc mvc;

    @MockitoBean JwtDecoder jwtDecoder;

    @Test
    void userRole_canAccessUserEndpoint() throws Exception {
        when(jwtDecoder.decode("user-token"))
                .thenReturn(jwtWithRoles("user-token", List.of("User")));

        mvc.perform(get("/user/hello").header(HttpHeaders.AUTHORIZATION, "Bearer user-token"))
                .andExpect(status().isOk());
    }

    @Test
    void adminRole_canAccessUserEndpoint() throws Exception {
        when(jwtDecoder.decode("admin-token"))
                .thenReturn(jwtWithRoles("admin-token", List.of("Admin")));

        mvc.perform(get("/user/hello").header(HttpHeaders.AUTHORIZATION, "Bearer admin-token"))
                .andExpect(status().isOk());
    }

    @Test
    void userRole_cannotAccessAdminEndpoint() throws Exception {
        when(jwtDecoder.decode("user-token"))
                .thenReturn(jwtWithRoles("user-token", List.of("User")));

        mvc.perform(get("/admin/hello").header(HttpHeaders.AUTHORIZATION, "Bearer user-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminRole_canAccessAdminEndpoint() throws Exception {
        when(jwtDecoder.decode("admin-token"))
                .thenReturn(jwtWithRoles("admin-token", List.of("Admin")));

        mvc.perform(get("/admin/hello").header(HttpHeaders.AUTHORIZATION, "Bearer admin-token"))
                .andExpect(status().isOk());
    }

    @Test
    void unauthenticatedRequest_returns401() throws Exception {
        mvc.perform(get("/admin/hello")).andExpect(status().isUnauthorized());
    }

    // helper to craft a JWT the security chain will accept
    private static Jwt jwtWithRoles(String token, List<String> roles) {
        Instant now = Instant.now();
        return new Jwt(
                token,
                now,
                now.plusSeconds(3600),
                Map.of("alg", "RS256"),
                Map.of(
                        "sub",
                        "user",
                        "iss",
                        "https://issuer.test",
                        "aud",
                        List.of("test-aud"),
                        "roles",
                        roles));
    }

    // ---- Test-only controllers registered in this slice ----

    @RestController
    @RequestMapping("/user")
    @PreAuthorize("hasAnyRole('User','Admin')")
    public static class UserRoleController {
        @GetMapping("/hello")
        public String hello() {
            return "ok";
        }
    }

    @RestController
    @RequestMapping("/admin")
    @PreAuthorize("hasRole('Admin')")
    public static class AdminRoleController {
        @GetMapping("/hello")
        public String hello() {
            return "ok";
        }
    }
}
