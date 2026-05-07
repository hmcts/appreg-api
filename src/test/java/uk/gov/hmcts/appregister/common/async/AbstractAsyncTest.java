package uk.gov.hmcts.appregister.common.async;

import java.io.File;
import java.util.Arrays;
import org.hibernate.AssertionFailure;
import org.junit.jupiter.api.AfterEach;
import uk.gov.hmcts.appregister.common.util.AppRegTempFileUtil;

public class AbstractAsyncTest {

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
}
