package uk.gov.hmcts.appregister.config;

import static uk.gov.hmcts.appregister.config.SecurityConstants.*;

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
import org.springframework.security.oauth2.jwt.*;
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
    SecurityFilterChain securityFeatureChain(
            HttpSecurity http, JwtAuthenticationConverter jwtAuthConverter) throws Exception {
        return http.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(
                        auth ->
                                auth.requestMatchers(SWAGGER_UI, OPENAPI_DOCS, HEALTH)
                                        .permitAll()
                                        .requestMatchers(ADMIN_ENDPOINT)
                                        .hasRole(ADMIN_ROLE)
                                        .requestMatchers(USER_ENDPOINT)
                                        .hasAnyRole(USER_ROLE, ADMIN_ROLE)
                                        .anyRequest()
                                        .authenticated())
                .oauth2ResourceServer(
                        oauth ->
                                oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthConverter))
                                        .authenticationEntryPoint(
                                                (req, res, ex) -> res.sendError(ERR_AUTH_REQUIRED)))
                .exceptionHandling(
                        e -> e.accessDeniedHandler((req, res, ex) -> res.sendError(ERR_FORBIDDEN)))
                .build();
    }

    /**
     * Configures how roles are extracted from JWTs. By default, Spring only maps "scp"/"scope"
     * claims. This tells Spring to also look at the "roles" claim (JusticeAAD app roles).
     */
    @Bean
    JwtAuthenticationConverter jwtAuthConverter() {
        var authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthoritiesClaimName(CLAIM_ROLES);
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
        var decoder = NimbusJwtDecoder.withIssuerLocation(issuer).build();
        var withIssuer = JwtValidators.createDefaultWithIssuer(issuer);
        decoder.setJwtValidator(
                new DelegatingOAuth2TokenValidator<>(withIssuer, audienceValidator));
        return decoder;
    }

    /**
     * Validates the "aud" (audience) claim to ensure the token was issued for this API. Spring
     * checks issuer/signature/expiry by default, but not audience, so this bean enforces that the
     * configured expected-audience is present in the token.
     */
    @Bean
    OAuth2TokenValidator<Jwt> audienceValidator(
            @Value("${app.security.expected-audience}") String expectedAud) {
        return token ->
                token.getAudience().contains(expectedAud)
                        ? OAuth2TokenValidatorResult.success()
                        : OAuth2TokenValidatorResult.failure(
                                new OAuth2Error(INVALID_TOKEN, AUD_MISMATCH_MESSAGE, null));
    }
}
