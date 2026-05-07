package uk.gov.hmcts.appregister.common.async.reader;

import java.io.IOException;
import java.util.List;
import uk.gov.hmcts.appregister.common.async.JobContext;

/**
 * Represents a page of data that has been read.
 */
@FunctionalInterface
public interface PageReader<T> {
    /**
     * reads the data from the page.
     *
     * @param relatedData The related page of data
     * @param jobContext The job context to log any read specific errors.
     */
    void readData(List<T> relatedData, JobContext jobContext) throws IOException;
}
