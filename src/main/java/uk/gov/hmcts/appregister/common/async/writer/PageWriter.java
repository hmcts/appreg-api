package uk.gov.hmcts.appregister.common.async.writer;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import uk.gov.hmcts.appregister.common.async.JobContext;

/**
 * A writer that writes a page of data.
 */
public interface PageWriter<T> extends Closeable {

    /**
     * Writes a page of data to the underlying stream.
     *
     * @param data The data to write.
     * @param context The job context to log any write specific errors.
     */
    void write(List<T> data, JobContext context) throws IOException;

    /**
     * gets the input stream for the data.
     *
     * @throws IOException Any problems getting the input stream.
     */
    InputStream getInputStream() throws IOException;
}
