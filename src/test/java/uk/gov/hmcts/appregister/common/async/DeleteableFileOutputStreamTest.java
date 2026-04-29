package uk.gov.hmcts.appregister.common.async;

import java.io.File;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.appregister.common.util.AppRegTempFileUtil;

public class DeleteableFileOutputStreamTest extends AbstractAsyncTest {

    @Test
    public void testDeleteFile() throws Exception {
        File file = AppRegTempFileUtil.generateTempFile("deleteable-output-stream-test");
        Assertions.assertTrue(file.exists());

        try (DeleteableFileOutputStream stream = new DeleteableFileOutputStream(file)) {
            stream.write("Test".getBytes(StandardCharsets.UTF_8));
            Assertions.assertTrue(file.length() > 0);
        }
        Assertions.assertFalse(file.exists());
    }
}
