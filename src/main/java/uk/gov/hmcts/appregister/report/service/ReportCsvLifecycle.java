package uk.gov.hmcts.appregister.report.service;

import com.opencsv.CSVWriterBuilder;
import com.opencsv.ICSVWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import uk.gov.hmcts.appregister.common.async.lifecycle.AsyncJobLifecycle;
import uk.gov.hmcts.appregister.common.async.lifecycle.AsyncJobLifecycleEvent;
import uk.gov.hmcts.appregister.common.util.AppRegTempFileUtil;

abstract class ReportCsvLifecycle<T> implements AsyncJobLifecycle<T> {
    private final File file;
    private final String reportTitle;
    private final String[] headers;
    private boolean headersWritten;

    ReportCsvLifecycle(String filePrefix, String reportTitle, String[] headers) throws IOException {
        this.file = AppRegTempFileUtil.generateTempFile(filePrefix);
        this.reportTitle = reportTitle;
        this.headers = headers;
    }

    @Override
    public void processing(AsyncJobLifecycleEvent<T> event) throws IOException {
        ensureHeadersWritten();

        try (ICSVWriter writer = createWriter()) {
            for (T row : event.getData()) {
                writer.writeNext(toCsvRow(row), false);
            }
        }
    }

    @Override
    public void completed(AsyncJobLifecycleEvent<T> event) throws IOException {
        ensureHeadersWritten();

        try (FileInputStream inputStream = new FileInputStream(file)) {
            event.getResponse().write(inputStream);
        } finally {
            close();
        }
    }

    @Override
    public void failed(AsyncJobLifecycleEvent<T> event) throws IOException {
        close();
    }

    protected abstract String[] toCsvRow(T row);

    File outputFile() {
        return file;
    }

    private void ensureHeadersWritten() throws IOException {
        if (headersWritten) {
            return;
        }

        try (ICSVWriter writer = createWriter()) {
            writer.writeNext(new String[] {reportTitle}, false);
            writer.writeNext(headers, false);
        }

        headersWritten = true;
    }

    private ICSVWriter createWriter() throws IOException {
        return new CSVWriterBuilder(
                        Files.newBufferedWriter(
                                file.toPath(),
                                StandardCharsets.UTF_8,
                                StandardOpenOption.CREATE,
                                StandardOpenOption.APPEND))
                .withSeparator(',')
                .build();
    }

    private void close() throws IOException {
        Files.deleteIfExists(file.toPath());
    }
}
