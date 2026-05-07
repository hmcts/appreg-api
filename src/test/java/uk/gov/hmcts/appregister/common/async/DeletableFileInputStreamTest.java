package uk.gov.hmcts.appregister.common.async;

import java.io.File;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.appregister.common.util.AppRegTempFileUtil;

public class DeletableFileInputStreamTest extends AbstractAsyncTest {
    @Test
    public void testDeleteFile() throws Exception {
        File file = AppRegTempFileUtil.generateTempFile("deleteable-input-stream-test");
        Assertions.assertTrue(file.exists());

        try (DeleteableFileInputStream stream = new DeleteableFileInputStream(file)) {
            DeleteableFileOutputStream out = new DeleteableFileOutputStream(file);
            out.write("Test".getBytes(StandardCharsets.UTF_8));
            Assertions.assertTrue(file.length() > 0);
            byte[] contentBytes = new byte[4];
            stream.read(contentBytes);
            Assertions.assertEquals("T", Character.toString(contentBytes[0]));
            Assertions.assertEquals("e", Character.toString(contentBytes[1]));
            Assertions.assertEquals("s", Character.toString(contentBytes[2]));
            Assertions.assertEquals("t", Character.toString(contentBytes[3]));
        }

        Assertions.assertFalse(file.exists());
    }
}
