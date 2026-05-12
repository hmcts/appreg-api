package uk.gov.hmcts.appregister.common.async;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DeletableFileInputStreamTest extends AbstractAsyncTest {
    @Test
    public void testDeleteFile() throws Exception {
        File file = Files.createTempFile("deleteable-input-stream-test-", ".tmp").toFile();
        file.deleteOnExit();
        Assertions.assertTrue(file.exists());

        Files.writeString(file.toPath(), "Test", StandardCharsets.UTF_8);
        Assertions.assertTrue(file.length() > 0);

        try (DeleteableFileInputStream stream = new DeleteableFileInputStream(file)) {
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
