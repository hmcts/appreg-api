package uk.gov.hmcts.appregister.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.appregister.common.security.UserProvider;
import uk.gov.hmcts.appregister.testutils.BasePostgresIntegrationTest;

@Import({
    NoSecurityProfileIntegrationTest.UserRoleController.class,
    NoSecurityProfileIntegrationTest.AdminRoleController.class
})
@ActiveProfiles("nosecurity")
class NoSecurityProfileIntegrationTest extends BasePostgresIntegrationTest {

    @Autowired private MockMvc mockMvc;

    @Autowired private ApplicationContext applicationContext;

    @Autowired private UserProvider userProvider;

    @Test
    void protectedEndpoints_allowAnonymousRequests_whenNoSecurityProfileIsActive()
            throws Exception {
        mockMvc.perform(get("/user/hello")).andExpect(status().isOk());
        mockMvc.perform(get("/admin/hello")).andExpect(status().isOk());
    }

    @Test
    void noSecurityProfile_usesLocalIdentityWithoutJwtInfrastructure() {
        assertThat(applicationContext.getBeansOfType(JwtDecoder.class)).isEmpty();
        assertThat(userProvider.getUserId()).isEqualTo("local:nosecurity");
        assertThat(userProvider.getEmail()).isEqualTo("nosecurity@appreg.local");
        assertThat(userProvider.getRoles()).containsExactly("LOCAL_NO_SECURITY");
    }

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
