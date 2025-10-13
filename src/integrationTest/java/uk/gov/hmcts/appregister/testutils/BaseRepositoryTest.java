package uk.gov.hmcts.appregister.testutils;

import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Base class for repository integration tests.
 *
 * <p>Sets up a minimal authenticated Spring Security context before each test by installing a
 * {@link org.springframework.security.oauth2.jwt.Jwt}-backed {@link
 * org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken}. This
 * mirrors the application’s runtime security so that repository calls which rely on the current
 * user (e.g. auditing fields like createdBy/changedBy, row-level security, or tenant scoping)
 * behave as expected.
 *
 * <p>Extends {@code BasePostgresIntegrationTest} to reuse the shared Testcontainers / Postgres
 * wiring and any common Flyway/database setup for integration tests.
 *
 * <p>Usage: extend this class in repository tests to get an authenticated context out of the box—no
 * need to re-create tokens in each test.
 */
public class BaseRepositoryTest extends BaseIntegration {

    @BeforeEach
    public void setUp() {

        Jwt jwt =
                Jwt.withTokenValue("test-token")
                        .header("alg", "none")
                        .claim("tid", TID)
                        .claim("oid", OID)
                        .claim("preferred_username", EMAIL)
                        .build();

        var auth = new JwtAuthenticationToken(jwt, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
