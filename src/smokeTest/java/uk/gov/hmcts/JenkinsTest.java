package uk.gov.hmcts;

import java.util.logging.Logger;
import org.junit.jupiter.api.Test;

class JenkinsTest {

    private static final Logger logger = Logger.getLogger(JenkinsTest.class.getName());

    @Test
    void test() {
        logger.info("Jenkins test executed successfully.");
    }
}
