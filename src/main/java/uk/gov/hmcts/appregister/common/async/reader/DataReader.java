package uk.gov.hmcts.appregister.common.async.reader;

import uk.gov.hmcts.appregister.common.async.model.JobTypeRequest;

import java.io.Closeable;
import java.io.InputStream;

/**
 * Allows us to import the job.
 */
@FunctionalInterface
public interface DataReader<T> extends Closeable {

    /**
     * reads the data from the reader and converts it to a list of objects.
      * @return The list of objects.
     */
    void readData(JobTypeRequest request, Class<T> cls, PageRead<T> pageImporter);

    /**
     * gets the input stream that has built up each time a page is processed.
     * @return The input stream.
     */
    InputStream getInputStream();
}
