package uk.gov.hmcts.appregister.testutils;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.time.OffsetDateTime;
import java.util.UUID;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import uk.gov.hmcts.appregister.testutils.docker.PostgresCommand;
import uk.gov.hmcts.appregister.testutils.stubs.wiremock.DatabasePersistance;

/**
 * A base class that loads postgres test container and resets any data inserted before each test
 * progresses. The postgres container will only start once across all tests.
 *
 * <p>Use this base class if you need to test against a real database and you want to ensure that
 * the data is reset before each test.
 */
// load the local profile that will bootstrap the base line data
@ActiveProfiles({"int"})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Slf4j
public abstract class BasePostgresIntegrationTest {
    protected static PostgresCommand postgresCommand = new PostgresCommand();

    @Autowired private DatabaseReset reset;

    @Autowired protected DatabasePersistance persistance;

    @LocalServerPort protected String port;

    @BeforeEach
    public void beforeEachTest() {
        reset.resetSequences();
        reset.resetDbData();
    }

    @DynamicPropertySource
    static void registerPgProperties(DynamicPropertyRegistry registry) {
        postgresCommand.start(registry);
    }

    protected URL getLocalUrlWithDate(String context, OffsetDateTime date)
            throws MalformedURLException {
        return URI.create(
                        "http://localhost:" + port + "/" + context + "?date=" + date.toLocalDate())
                .toURL();
    }

    /*protected URL getLocalUrlWithId(String context, UUID id)
        throws MalformedURLException {
        return URI.create(
                "http://localhost:" + port + "/" + context + "?id=" + id)
            .toURL();
    }*/

    protected URL getLocalUrl(String context) throws MalformedURLException {
        return URI.create("http://localhost:" + port + "/" + context).toURL();
    }
}
