package uk.gov.hmcts.appregister.common.async;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;

/**
 * A closeable input stream that is backed by a file and will delete when closed.
 */
public class DeleteableFileInputStream extends FileInputStream {
    private File file;

    public DeleteableFileInputStream(File file) throws IOException {
        super(file);
        this.file = file;
    }

    @Override
    public void close() throws IOException {
        super.close();

        // delete the file
        Files.deleteIfExists(file.toPath());
    }
}
