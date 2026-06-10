package uk.gov.hmcts.appregister.health;

import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.appregister.testutils.BaseIntegration;

/**
 * Built-in feature which saves service's swagger specs in temporary directory. Each CI run on
 * master should automatically save and upload (if updated) documentation.
 */
class OpenApiPublisherTest extends BaseIntegration {

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private MockMvc mvc;

    @BeforeAll
    static void before() {
        // stop so that when started functional data is inserted
        postgresCommand.stop();
    }
}
