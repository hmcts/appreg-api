package uk.gov.hmcts.appregister.config;

import static uk.gov.hmcts.appregister.config.SecurityConstants.AUD_MISMATCH_MESSAGE;
import static uk.gov.hmcts.appregister.config.SecurityConstants.ERR_AUTH_REQUIRED;
import static uk.gov.hmcts.appregister.config.SecurityConstants.ERR_FORBIDDEN;
import static uk.gov.hmcts.appregister.config.SecurityConstants.HEALTH;
import static uk.gov.hmcts.appregister.config.SecurityConstants.INVALID_TOKEN;
import static uk.gov.hmcts.appregister.config.SecurityConstants.OPENAPI_DOCS;
import static uk.gov.hmcts.appregister.config.SecurityConstants.ROLE_CLAIM;
import static uk.gov.hmcts.appregister.config.SecurityConstants.ROLE_PREFIX;
import static uk.gov.hmcts.appregister.config.SecurityConstants.SWAGGER_UI;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    /**
     * Defines the main Spring Security filter chain for the API. - Disables CSRF (not needed for
     * stateless JWT-based APIs). - Secures endpoints based on roles from the "roles" claim. -
     * Exposes Swagger/OpenAPI/health endpoints without authentication. - Configures JWT as the auth
     * mechanism. - Maps authentication failures (401) and authorization failures (403).
     */
    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http, JwtAuthenticationConverter jwtAuthConverter) throws Exception {

        http.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(
                        auth ->
                                auth.requestMatchers(SWAGGER_UI, OPENAPI_DOCS, HEALTH)
                                        .permitAll()
                                        .anyRequest()
                                        .authenticated())
                .oauth2ResourceServer(
                        oauth ->
                                oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthConverter))
                                        .authenticationEntryPoint(
                                                (req, res, ex) -> res.sendError(ERR_AUTH_REQUIRED)))
                .exceptionHandling(
                        e -> e.accessDeniedHandler((req, res, ex) -> res.sendError(ERR_FORBIDDEN)));

        return http.build();
    }

    /**
     * Configures how roles are extracted from JWTs. By default, Spring only maps "scp"/"scope"
     * claims. This tells Spring to also look at the "roles" claim (JusticeAAD app roles).
     */
    @Bean
    JwtAuthenticationConverter jwtAuthConverter() {
        var authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthoritiesClaimName(ROLE_CLAIM);
        authoritiesConverter.setAuthorityPrefix(ROLE_PREFIX);

        var authenticationConverter = new JwtAuthenticationConverter();
        authenticationConverter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        return authenticationConverter;
    }

    /**
     * Creates the JwtDecoder that verifies tokens against the issuer’s JWKS. It enforces the
     * standard issuer/expiry validation and chains in the custom audienceValidator so only tokens
     * signed by the trusted issuer and targeted at this API are accepted.
     */
    @Bean
    JwtDecoder jwtDecoder(
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuer,
            OAuth2TokenValidator<Jwt> audienceValidator) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withIssuerLocation(issuer).build();
        decoder.setJwtValidator(buildValidator(issuer, audienceValidator));
        return decoder;
    }

    /**
     * Creates a token validator that checks both issuer and expiration (by default), and adds a
     * custom audience validator.
     *
     * <p>Note: issuer and expiration validation are included automatically by {@code
     * createDefaultWithIssuer(issuer)}.
     *
     * @param issuer expected issuer ("iss" claim)
     * @param audienceValidator custom validator for the "aud" claim
     * @return combined validator enforcing issuer, expiration, and audience
     */
    OAuth2TokenValidator<Jwt> buildValidator(
            String issuer, OAuth2TokenValidator<Jwt> audienceValidator) {
        OAuth2TokenValidator<Jwt> defaultValidator = JwtValidators.createDefaultWithIssuer(issuer);
        return new DelegatingOAuth2TokenValidator<>(defaultValidator, audienceValidator);
    }

    /**
     * Validates the "aud" (audience) claim to ensure the token was issued for this API. Spring
     * checks issuer/signature/expiry by default, but not audience, so this bean enforces that the
     * configured expected-audience is present in the token.
     */
    @Bean
    OAuth2TokenValidator<Jwt> audienceValidator(
            @Value("${app.security.expected-audience}") String expectedAud) {
        return token -> {
            List<String> audience = token.getAudience();
            boolean valid = audience != null && audience.contains(expectedAud);

            return valid
                    ? OAuth2TokenValidatorResult.success()
                    : OAuth2TokenValidatorResult.failure(
                            new OAuth2Error(INVALID_TOKEN, AUD_MISMATCH_MESSAGE, null));
        };
    }
}
