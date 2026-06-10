package uk.gov.hmcts.appregister.testutils;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.time.OffsetDateTime;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.AssertionFailure;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.wiremock.spring.ConfigureWireMock;
import org.wiremock.spring.EnableWireMock;
import uk.gov.hmcts.appregister.common.util.AppRegTempFileUtil;
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
@ActiveProfiles({"testing", "int"})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Slf4j
@EnableWireMock({@ConfigureWireMock(port = 0)})
@AutoConfigureMockMvc
@DirtiesContext
public abstract class BasePostgresIntegrationTest {
    protected static PostgresCommand postgresCommand = new PostgresCommand();

    @Autowired private DatabaseReset reset;

    @Autowired protected DatabasePersistance persistance;

    @LocalServerPort protected String port;

    @BeforeEach
    void beforeEachTest() {
        reset.resetDbData();
    }

    @AfterEach
    void tearDown() {
        // ensure that we do not leave any temp files around.
        if (AppRegTempFileUtil.doesTempFileExist()) {
            File[] tempFiles = AppRegTempFileUtil.getTempFilesThatExist();

            // mark for deletion when the process ends
            Arrays.asList(tempFiles).forEach(File::deleteOnExit);

            throw new AssertionFailure(
                    "You're code is not clearing up temp files that it creates, please make sure "
                            + "you delete files by wrapping code in try/resources where necessary. "
                            + "Found temp files: "
                            + Arrays.stream(tempFiles).map(File::getAbsolutePath).toList());
        }
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

    protected URL getLocalUrl(String context) throws MalformedURLException {
        return URI.create("http://localhost:" + port + "/" + context).toURL();
    }
}
