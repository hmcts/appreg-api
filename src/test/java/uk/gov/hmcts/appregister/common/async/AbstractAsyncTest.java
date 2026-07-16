package uk.gov.hmcts.appregister.common.async;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Arrays;
import org.hibernate.AssertionFailure;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import uk.gov.hmcts.appregister.common.util.AppRegTempFileUtil;

public abstract class AbstractAsyncTest {

    @AfterEach
    void tearDown() {
        // ensure that we do not leave any temp files around.
        if (AppRegTempFileUtil.doesTempFileExist()) {
            // mark for deletion when the process ends
            Arrays.asList(AppRegTempFileUtil.getTempFilesThatExist()).forEach(File::deleteOnExit);

            throw new AssertionFailure(
                    "You're code is not clearing up temp files that it creates, please make sure "
                            + "you delete files by wrapping code in try/resources where necessary.");
        }
    }

    protected File testResourceFile(String resourceName) {
        URL resource = Thread.currentThread().getContextClassLoader().getResource(resourceName);
        Assertions.assertNotNull(resource, "Test resource not found: " + resourceName);

        try {
            return Path.of(resource.toURI()).toFile();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid test resource URI: " + resourceName, e);
        }
    }
}
