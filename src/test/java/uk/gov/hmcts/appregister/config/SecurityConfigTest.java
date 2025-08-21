package uk.gov.hmcts.appregister.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class SecurityConfigTest {

    SecurityConfig config = new SecurityConfig();

    @Test
    void audienceValidator_expectedAudiencePresentInJwt_resultContainNoErrors() {
        OAuth2TokenValidator<Jwt> validator = config.audienceValidator("expected-aud");

        Jwt jwt =
                new Jwt(
                        "t",
                        Instant.now(),
                        Instant.now().plusSeconds(60),
                        Map.of("alg", "none"),
                        Map.of("aud", List.of("expected-aud")));

        OAuth2TokenValidatorResult result = validator.validate(jwt);
        assertThat(result.hasErrors()).isFalse();
    }

    @Test
    void audienceValidator_invalidAudiencePresentInJwt_resultContainsError() {
        var validator = config.audienceValidator("expected-aud");

        Jwt jwt =
                new Jwt(
                        "t",
                        Instant.now(),
                        Instant.now().plusSeconds(60),
                        Map.of("alg", "none"),
                        Map.of("aud", List.of("wrong-aud")));

        OAuth2TokenValidatorResult result = validator.validate(jwt);
        assertThat(result.hasErrors()).isTrue();
        assertThat(result.getErrors())
                .extracting(OAuth2Error::getErrorCode)
                .contains("invalid_token");
    }

    @Test
    void jwtAuthConverter_mapsRolesClaimToAuthorities() {
        JwtAuthenticationConverter converter = config.jwtAuthConverter();

        Jwt jwt =
                new Jwt(
                        "t",
                        Instant.now(),
                        Instant.now().plusSeconds(60),
                        Map.of("alg", "none"),
                        Map.of("roles", List.of("Admin", "User")));

        AbstractAuthenticationToken auth = converter.convert(jwt);

        assertThat(auth).isNotNull();
        assertThat(auth.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_Admin", "ROLE_User");
    }

    @Test
    @SuppressWarnings("unchecked")
    void buildValidator_combinesIssuerAndAudienceValidator() throws Exception {
        OAuth2TokenValidator<Jwt> audienceValidator = mock(OAuth2TokenValidator.class);
        String issuer = "https://example-issuer";

        OAuth2TokenValidator<Jwt> combined = config.buildValidator(issuer, audienceValidator);

        assertThat(combined).isInstanceOf(DelegatingOAuth2TokenValidator.class);

        Field field = DelegatingOAuth2TokenValidator.class.getDeclaredField("tokenValidators");
        field.setAccessible(true);

        Collection<OAuth2TokenValidator<Jwt>> validators =
                (Collection<OAuth2TokenValidator<Jwt>>) field.get(combined);

        assertThat(validators).hasSize(2);
        assertThat(validators).anyMatch(v -> v == audienceValidator);
    }
}
