package uk.gov.hmcts.appregister.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

class SecurityConfigTest {
    private static final Instant ISSUED_AT = Instant.parse("2025-01-02T03:04:05Z");
    private static final Instant EXPIRES_AT = Instant.parse("2025-01-02T03:05:05Z");

    SecurityConfig config = new SecurityConfig();

    @Test
    void jwtAuthConverter_mapsRolesClaimToAuthorities() {
        JwtAuthenticationConverter converter = config.jwtAuthConverter();

        Jwt jwt =
                new Jwt(
                        "t",
                        ISSUED_AT,
                        EXPIRES_AT,
                        Map.of("alg", "none"),
                        Map.of("roles", List.of("Admin", "User")));

        AbstractAuthenticationToken auth = converter.convert(jwt);

        assertThat(auth).isNotNull();
        assertThat(auth.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsAll(List.of("ROLE_Admin", "ROLE_User"));
    }
}
