package uk.gov.hmcts;

import java.util.logging.Logger;

import groovy.util.logging.Slf4j;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.appregister.applicationcode.exception.AppCodeError;

class JenkinsTest {

    private static final Logger logger = Logger.getLogger(JenkinsTest.class.getName());

    @Test
    void test() {
        logger.info("Jenkins test executed successfully.");
    }
}
