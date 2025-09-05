package uk.gov.hmcts.appregister.testutils;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import uk.gov.hmcts.appregister.common.entity.base.Accountable;
import uk.gov.hmcts.appregister.common.entity.base.Changeable;
import uk.gov.hmcts.appregister.common.entity.base.Versionable;
import uk.gov.hmcts.appregister.testutils.docker.PostgresCommand;
import uk.gov.hmcts.appregister.testutils.stub.DatabasePersistance;

/**
 * A base class that loads postgres test container and resets any data inserted before each test
 * progresses. The postgres container will only start once across all tests.
 *
 * <p>Use this base class if you need to test against a real database and you want to ensure that
 * the data is reset before each test.
 */
// load the local profile that will bootstrap the base line data
@ActiveProfiles({"local"})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Slf4j
public abstract class BasePostgresIntegrationTest {
    protected static PostgresCommand postgresCommand = new PostgresCommand();

    @Autowired private DatabaseReset reset;

    @Autowired protected DatabasePersistance persistance;

    @BeforeEach
    public void beforeEachTest() {
        reset.resetSequences();
        reset.resetDbData();
    }

    @DynamicPropertySource
    static void registerPgProperties(DynamicPropertyRegistry registry) {
        postgresCommand.start(registry);
    }

    public void expectAccountable(Accountable expected, Accountable actual) {
        Assertions.assertEquals(expected.getCreatedUser(), actual.getCreatedUser());
    }

    public void expectVersionable(Versionable expected, Versionable actual) {
        Assertions.assertEquals(expected.getVersion(), actual.getVersion());
    }

    public void expectChangeable(Changeable expected, Changeable actual) {
        Assertions.assertEquals(expected.getChangedBy(), actual.getChangedBy());
        Assertions.assertTrue(
                DateUtil.equalsIgnoreMillis(expected.getChangedDate(), actual.getChangedDate()));
    }

    public void expectAllCommonEntityFields(Object expected, Object actual) {
        if (expected instanceof Accountable expectedAccountable
                && actual instanceof Accountable actualAccountable) {
            expectAccountable(expectedAccountable, actualAccountable);
        }
        if (expected instanceof Versionable expectedVersionable
                && actual instanceof Versionable actualVersionable) {
            expectVersionable(expectedVersionable, actualVersionable);
        }
        if (expected instanceof Changeable expectedChangeable
                && actual instanceof Changeable actualChangeable) {
            expectChangeable(expectedChangeable, actualChangeable);
        }
    }
}
