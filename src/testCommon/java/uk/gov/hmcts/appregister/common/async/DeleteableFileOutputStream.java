package uk.gov.hmcts.appregister.common.async;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;

/**
 * A closeable output stream that is backed by a file and will delete when closed.
 */
public class DeleteableFileOutputStream extends FileOutputStream {
    private File file;

    public DeleteableFileOutputStream(File file) throws IOException {
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
