package uk.gov.hmcts.appregister.config;

import static uk.gov.hmcts.appregister.config.SecurityConstants.ERR_AUTH_REQUIRED;
import static uk.gov.hmcts.appregister.config.SecurityConstants.ERR_FORBIDDEN;
import static uk.gov.hmcts.appregister.config.SecurityConstants.HEALTH;
import static uk.gov.hmcts.appregister.config.SecurityConstants.OPENAPI;
import static uk.gov.hmcts.appregister.config.SecurityConstants.REST_IMPLEMENTATION_HEALTH;
import static uk.gov.hmcts.appregister.config.SecurityConstants.ROLE_CLAIM;
import static uk.gov.hmcts.appregister.config.SecurityConstants.ROLE_PREFIX;
import static uk.gov.hmcts.appregister.config.SecurityConstants.SWAGGER_UI;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuration for securing the API using Spring Security and JWTs.
 */
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

        http.authorizeHttpRequests(
                        auth ->
                                auth.requestMatchers(
                                                SWAGGER_UI,
                                                OPENAPI,
                                                HEALTH,
                                                REST_IMPLEMENTATION_HEALTH)
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
}
