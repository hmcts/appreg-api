package uk.gov.hmcts.appregister.config;

import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.TEXT_PLAIN;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.*;

/**
 * MVC slice that exercises SecurityConfig’s filter chain end-to-end.
 * The nested controllers are **test-only** and exist solely to give the requests a handler.
 */
@WebMvcTest(controllers = {
    SecurityConfigTest.OpenController.class,
    SecurityConfigTest.UserController.class,
    SecurityConfigTest.AdminController.class,
    SecurityConfigTest.SecureController.class
})
@Import(SecurityConfig.class)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.security.oauth2.resourceserver.jwt.issuer-uri=https://issuer.test",
    "app.security.expected-audience=test-aud"
})
class SecurityConfigTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    JwtDecoder jwtDecoder;

    private Jwt jwt(String tokenValue, List<String> roles, String aud) {
        Instant now = Instant.now();
        return new Jwt(
            tokenValue, now, now.plusSeconds(3600),
            Map.of("alg", "RS256"),
            Map.of("sub","user", "iss","https://issuer.test", "aud", List.of(aud), "roles", roles)
        );
    }

    @Test
    @DisplayName("permitAll: /actuator/health, /v3/api-docs, /swagger-ui/**")
    void permitAllEndpoints() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Unauthenticated request to protected endpoint -> 401")
    void unauthenticatedIs401() throws Exception {
        mvc.perform(get("/api/secure"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("USER role allowed on /api/user/**, forbidden on /api/admin/**")
    void userRoleAccess() throws Exception {
        when(jwtDecoder.decode("user-token"))
            .thenReturn(jwt("user-token", List.of("USER"), "test-aud"));

        mvc.perform(get("/api/user/hello")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer user-token"))
            .andExpect(status().isOk())
            .andExpect(content().string("hi user"));

        mvc.perform(get("/api/admin/hello")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer user-token"))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ADMIN role allowed on /api/admin/**")
    void adminRoleAccess() throws Exception {
        when(jwtDecoder.decode("admin-token"))
            .thenReturn(jwt("admin-token", List.of("ADMIN"), "test-aud"));

        mvc.perform(get("/api/admin/hello")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer admin-token"))
            .andExpect(status().isOk())
            .andExpect(content().string("hi admin"));
    }

    @Test
    @DisplayName("Bad JWT -> 401 (authenticationEntryPoint)")
    void badJwtIs401() throws Exception {
        // Only throw for this specific token so other tests aren't affected
        when(jwtDecoder.decode("bad-token")).thenThrow(new BadJwtException("boom"));

        mvc.perform(get("/api/user/hello").header(HttpHeaders.AUTHORIZATION, "Bearer bad-token"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("CSRF disabled: POST with JWT works without CSRF token")
    void csrfDisabledForJwtApi() throws Exception {
        when(jwtDecoder.decode("user-token"))
            .thenReturn(jwt("user-token", List.of("USER"), "test-aud"));

        mvc.perform(post("/api/user/echo")
                        .contentType(TEXT_PLAIN)
                        .content("echo-body")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer user-token"))
            .andExpect(status().isOk())
            .andExpect(content().string("echo-body"));
    }

    /* ---------- TEST-ONLY CONTROLLERS ---------- */

    @RestController
    static class OpenController {
        @GetMapping("/actuator/health") String health() { return "ok"; }
        @GetMapping("/v3/api-docs") String docs() { return "{}"; }
        @GetMapping("/swagger-ui/index.html") String swagger() { return "<html/>"; }
    }

    @RestController
    @RequestMapping("/api/user")
    static class UserController {
        // Optionally keep @PreAuthorize to also exercise @EnableMethodSecurity:
        @PreAuthorize("hasAnyRole('USER','ADMIN')")
        @GetMapping("/hello") String hello() { return "hi user"; }

        @PreAuthorize("hasAnyRole('USER','ADMIN')")
        @PostMapping("/echo") String echo(@RequestBody String body) { return body; }
    }

    @RestController
    @RequestMapping("/api/admin")
    static class AdminController {
        @PreAuthorize("hasRole('ADMIN')")
        @GetMapping("/hello") String hello() { return "hi admin"; }
    }

    @RestController
    @RequestMapping("/api")
    static class SecureController {
        @GetMapping("/secure") String secure() { return "secured"; }
    }
}
