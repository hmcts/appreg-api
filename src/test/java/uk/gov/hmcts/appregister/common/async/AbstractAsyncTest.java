package uk.gov.hmcts.appregister.common.async;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.hibernate.AssertionFailure;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import uk.gov.hmcts.appregister.common.util.AppRegTempFileUtil;

public abstract class AbstractAsyncTest {
    private Set<String> existingTempFiles;

    @BeforeEach
    void captureExistingTempFiles() {
        existingTempFiles =
                Arrays.stream(AppRegTempFileUtil.getTempFilesThatExist())
                        .map(File::getAbsolutePath)
                        .collect(Collectors.toSet());
    }

    @AfterEach
    void tearDown() {
        // ensure that we do not leave any temp files around.
        File[] leakedFiles =
                Arrays.stream(AppRegTempFileUtil.getTempFilesThatExist())
                        .filter(file -> !existingTempFiles.contains(file.getAbsolutePath()))
                        .toArray(File[]::new);

        if (leakedFiles.length > 0) {
            // mark for deletion when the process ends
            Arrays.asList(leakedFiles).forEach(File::deleteOnExit);

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
