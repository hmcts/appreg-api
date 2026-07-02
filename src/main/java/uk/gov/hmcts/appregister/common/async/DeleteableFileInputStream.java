package uk.gov.hmcts.appregister.common.async;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;

/**
 * A closeable input stream that is backed by a file and will delete when closed.
 */
public class DeleteableFileInputStream extends FileInputStream {
    private final File file;
    private boolean streamClosed;
    private boolean closing;

    public DeleteableFileInputStream(File file) throws IOException {
        super(file);
        this.file = file;
    }

    @Override
    public synchronized void close() throws IOException {
        if (!streamClosed) {
            if (closing) {
                return;
            }

            closing = true;
            try {
                super.close();
                streamClosed = true;
            } finally {
                closing = false;
            }
        }

        // delete the file
        Files.deleteIfExists(file.toPath());
    }
}
