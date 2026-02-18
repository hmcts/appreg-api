package uk.gov.hmcts.appregister.health;

import io.restassured.response.Response;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.appregister.testutils.BasePostgresIntegrationTest;
import uk.gov.hmcts.appregister.testutils.client.RestAssuredClient;

/**
 * A test that makes sure that the functional test environment loads under the functional Spring
 * profile.
 */
@ActiveProfiles({"functional", "int"})
public class FunctionalHealthCheckTest extends BasePostgresIntegrationTest {

    @Autowired protected RestAssuredClient restAssuredClient;

    @BeforeAll
    public static void before() {
        // stop so that when started functional data is inserted
        postgresCommand.stop();
    }

    @AfterAll
    public static void after() {
        // stop so that other tests can start using default profile testing profile data.
        postgresCommand.stop();
    }

    @Test
    public void healthCheck() throws Exception {
        Response response = restAssuredClient.executeGetRequest(getLocalUrl("health"), null, null);
        Assertions.assertEquals(200, response.getStatusCode());
    }
}
