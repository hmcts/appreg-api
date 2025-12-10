package uk.gov.hmcts.appregister.openapi;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.appregister.testutils.BasePostgresIntegrationTest;

/**
 * Built-in feature which saves service's swagger specs in temporary directory. Each CI run on
 * master should automatically save and upload (if updated) documentation.
 */
@AutoConfigureMockMvc
class OpenApiPublisherTest extends BasePostgresIntegrationTest {

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private MockMvc mvc;

    @DisplayName("Generate swagger documentation")
    @Test
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
    void generateDocs() throws Exception {
        mvc.perform(get("/specs/openapi.json")).andExpect(status().isOk());
    }
}
