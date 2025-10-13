package uk.gov.hmcts.appregister.testutils;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import uk.gov.hmcts.appregister.common.entity.base.Accountable;
import uk.gov.hmcts.appregister.common.entity.base.Changeable;
import uk.gov.hmcts.appregister.common.entity.base.Deletable;
import uk.gov.hmcts.appregister.common.entity.base.UnmanagedChangeable;
import uk.gov.hmcts.appregister.common.entity.base.Versionable;
import uk.gov.hmcts.appregister.common.enumeration.YesOrNo;
import uk.gov.hmcts.appregister.testutils.token.TokenGenerator;
import uk.gov.hmcts.appregister.util.DateUtil;

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
public class BaseRepositoryTest extends BasePostgresIntegrationTest {
    @BeforeEach
    public void setUp() throws Exception {
        Jwt jwt = TokenGenerator.builder().build().getJwtFromToken();
        var auth = new JwtAuthenticationToken(jwt, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    public void expectAccountable(Accountable actual) {
        Assertions.assertEquals(TokenGenerator.DEFAULT_USERNAME, actual.getCreatedUser());
    }

    public void expectVersionable(Versionable expected, Versionable actual) {
        Assertions.assertEquals(expected.getVersion(), actual.getVersion());
    }

    public void expectChangeable(Changeable expected, Changeable actual) {
        Assertions.assertEquals(
                TokenGenerator.DEFAULT_TID + ":" + TokenGenerator.DEFAULT_OID,
                actual.getChangedBy());
        Assertions.assertTrue(
                DateUtil.equalsIgnoreMillis(expected.getChangedDate(), actual.getChangedDate()));
    }

    public void expectAllCommonEntityFields(Object expected, Object actual) {
        if (expected.getClass() != actual.getClass()) {
            throw new IllegalArgumentException("Objects must be of the same class");
        }

        if (actual instanceof Accountable actualAccountable) {
            expectAccountable(actualAccountable);
        }
        if (expected instanceof Versionable expectedVersionable
                && actual instanceof Versionable actualVersionable) {
            expectVersionable(expectedVersionable, actualVersionable);
        }
        if (expected instanceof Changeable expectedChangeable
                && actual instanceof Changeable actualChangeable) {
            expectChangeable(expectedChangeable, actualChangeable);
        } else if (expected instanceof UnmanagedChangeable expectedUnmanagedChangeable
                && actual instanceof UnmanagedChangeable actualUnmanagedChangeable) {
            assertThat(expectedUnmanagedChangeable.getChangedBy())
                    .isEqualTo(actualUnmanagedChangeable.getChangedBy());
            Assertions.assertTrue(
                    DateUtil.equalsIgnoreMillis(
                            expectedUnmanagedChangeable.getChangedDate(),
                            actualUnmanagedChangeable.getChangedDate()));
        }

        if (expected instanceof Deletable expectedDeletable
                && actual instanceof Deletable actualUnmanagedChangeable) {
            assertThat(actualUnmanagedChangeable.getDeleted())
                    .isEqualTo(actualUnmanagedChangeable.getDeleted());

            if (actualUnmanagedChangeable.getDeleted() == YesOrNo.YES) {
                Assertions.assertTrue(
                        DateUtil.equalsIgnoreMillis(
                                expectedDeletable.getDeletedDate(),
                                actualUnmanagedChangeable.getDeletedDate()));
                assertThat(actualUnmanagedChangeable.getDeletedBy())
                        .isEqualTo(TokenGenerator.DEFAULT_TID + ":" + TokenGenerator.DEFAULT_OID);
            } else {
                assertThat(actualUnmanagedChangeable.getDeleted()).isNull();
                assertThat(actualUnmanagedChangeable.getDeletedBy()).isNull();
            }
        }
    }
}
