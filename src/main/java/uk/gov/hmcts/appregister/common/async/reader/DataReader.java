package uk.gov.hmcts.appregister.common.async.reader;

import java.io.Closeable;
import java.io.IOException;
import uk.gov.hmcts.appregister.common.async.JobContext;

/**
 * Allows us to read data from a source in pages.
 */
public interface DataReader<T> extends Closeable {
    /**
     * reads the page of data from the reader and converts it to a list of objects.
     *
     * @param position The position of the page to read.
     * @param pageReader The page of read data
     * @param jobContext The job context to log any read specific errors.
     */
    void readData(ReadPagePosition position, PageReader<T> pageReader, JobContext jobContext)
            throws IOException;
}
