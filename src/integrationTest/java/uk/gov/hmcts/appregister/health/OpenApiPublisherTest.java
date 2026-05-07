package uk.gov.hmcts.appregister.health;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
    public static void before() {
        // stop so that when started functional data is inserted
        postgresCommand.stop();
    }

    @DisplayName("Generate swagger documentation")
    @Test
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
    void generateDocs() throws Exception {
        mvc.perform(get("/specs/openapi.json")).andExpect(status().isOk());
    }
}
