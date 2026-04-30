package uk.gov.hmcts.appregister.report.service;

import com.opencsv.CSVWriterBuilder;
import com.opencsv.ICSVWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import uk.gov.hmcts.appregister.common.async.lifecycle.AsyncJobLifecycle;
import uk.gov.hmcts.appregister.common.async.lifecycle.AsyncJobLifecycleEvent;
import uk.gov.hmcts.appregister.common.util.AppRegTempFileUtil;
import uk.gov.hmcts.appregister.report.model.DurationReportRow;

class DurationReportLifecycle implements AsyncJobLifecycle<DurationReportRow> {
    private static final DateTimeFormatter LIST_DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String[] HEADERS = {
        "List Date",
        "List Court House Name",
        "List Other Location",
        "CJA Code",
        "List Description",
        "Duration Hours",
        "Duration Minutes"
    };

    private final File file;
    private boolean headersWritten;

    DurationReportLifecycle() throws IOException {
        this.file = AppRegTempFileUtil.generateTempFile("duration-report");
    }

    @Override
    public void processing(AsyncJobLifecycleEvent<DurationReportRow> event) throws IOException {
        writeRows(event.getData());
    }

    @Override
    public void completed(AsyncJobLifecycleEvent<DurationReportRow> event) throws IOException {
        ensureHeadersWritten();

        try (FileInputStream inputStream = new FileInputStream(file)) {
            event.getResponse().write(inputStream);
        } finally {
            close();
        }
    }

    @Override
    public void failed(AsyncJobLifecycleEvent<DurationReportRow> event) throws IOException {
        close();
    }

    private void writeRows(List<DurationReportRow> rows) throws IOException {
        ensureHeadersWritten();

        try (ICSVWriter writer = createWriter()) {
            for (DurationReportRow row : rows) {
                writer.writeNext(toCsvRow(row), false);
            }
        }
    }

    private void ensureHeadersWritten() throws IOException {
        if (headersWritten) {
            return;
        }

        try (ICSVWriter writer = createWriter()) {
            writer.writeNext(new String[] {"Duration Report"}, false);
            writer.writeNext(HEADERS, false);
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

    private String[] toCsvRow(DurationReportRow row) {
        return new String[] {
            formatListDate(row.getListDate()),
            Objects.toString(row.getCourthouseName(), ""),
            Objects.toString(row.getOtherCourthouse(), ""),
            Objects.toString(row.getCjaCode(), ""),
            Objects.toString(row.getListDescription(), ""),
            Objects.toString(row.getDurationHours(), ""),
            Objects.toString(row.getDurationMinutes(), "")
        };
    }

    private String formatListDate(LocalDate value) {
        return value == null ? "" : LIST_DATE_FORMAT.format(value);
    }

    private void close() throws IOException {
        Files.deleteIfExists(file.toPath());
    }
}
