package uk.gov.hmcts.appregister.common.async.lifecycle;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import uk.gov.hmcts.appregister.common.async.model.CsvPojo;
import uk.gov.hmcts.appregister.common.async.writer.CsvWriter;

/**
 * An abstract implementation of the AsyncLifecycle interface that provides a template for
 * processing and completing lifecycle events specifically when writing records to a csv file.
 */
public abstract class AbstractCsvWriterAsyncLifecycle<T, R extends CsvPojo>
        implements AsyncJobLifecycle<T> {
    private final CsvWriter<R> csvWriter;

    /**
     * Create a new instance of the AbstractCsvWriterAsyncLifecycle.
     *
     * @param csvWriter The csv writer to use.
     */
    protected AbstractCsvWriterAsyncLifecycle(CsvWriter<R> csvWriter) {
        this.csvWriter = csvWriter;
    }

    @Override
    public void processing(AsyncJobLifecycleEvent<T> event) throws IOException {
        List<T> dataToWrite = event.getData();
        List<R> convertedData = new ArrayList<>();

        for (T convertFrom : dataToWrite) {
            convertedData.add(convert(convertFrom));
        }

        // write the data
        csvWriter.write(convertedData, event.getContext());
    }

    /**
     * converts the data to the csv pojo.
     *
     * @return The csv bean that we need to write.
     */
    protected abstract R convert(T data) throws IOException;

    /**
     * writes the csv file to the underlying job (which will persist to the database). Once written
     * the underlying file will be removed from the file system.
     *
     * @param event The lifecycle event.
     */
    @Override
    public void completed(AsyncJobLifecycleEvent<T> event) throws IOException {
        try (csvWriter) {
            event.getResponse().write(csvWriter.getInputStream());
        }
    }

    @Override
    public void failed(AsyncJobLifecycleEvent<T> event) throws IOException {
        csvWriter.close();
    }
}
