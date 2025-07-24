package uk.gov.hmcts;

import org.junit.jupiter.api.Test;
import java.util.logging.Logger;

class JenkinsTest {

    private static final Logger logger = Logger.getLogger(JenkinsTest.class.getName());

    @Test
    void test() {
        logger.info("Jenkins test executed successfully.");
    }
}
