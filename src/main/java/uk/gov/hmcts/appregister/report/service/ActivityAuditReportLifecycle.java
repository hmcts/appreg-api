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
import java.util.List;
import java.util.Objects;
import uk.gov.hmcts.appregister.common.async.lifecycle.AsyncJobLifecycle;
import uk.gov.hmcts.appregister.common.async.lifecycle.AsyncJobLifecycleEvent;
import uk.gov.hmcts.appregister.common.util.AppRegTempFileUtil;
import uk.gov.hmcts.appregister.report.model.ActivityAuditReportRow;

class ActivityAuditReportLifecycle implements AsyncJobLifecycle<ActivityAuditReportRow> {
    private static final String[] HEADERS = {
        "Event Name",
        "Table Name",
        "Column Name",
        "Old Value",
        "New Value",
        "Created Date",
        "User Name"
    };

    private final File file;
    private boolean headersWritten;

    ActivityAuditReportLifecycle() throws IOException {
        this.file = AppRegTempFileUtil.generateTempFile("activity-audit-report");
    }

    @Override
    public void processing(AsyncJobLifecycleEvent<ActivityAuditReportRow> event)
            throws IOException {
        writeRows(event.getData());
    }

    @Override
    public void completed(AsyncJobLifecycleEvent<ActivityAuditReportRow> event) throws IOException {
        ensureHeadersWritten();

        try (FileInputStream inputStream = new FileInputStream(file)) {
            event.getResponse().write(inputStream);
        } finally {
            close();
        }
    }

    @Override
    public void failed(AsyncJobLifecycleEvent<ActivityAuditReportRow> event) throws IOException {
        close();
    }

    private void writeRows(List<ActivityAuditReportRow> rows) throws IOException {
        ensureHeadersWritten();

        try (ICSVWriter writer = createWriter()) {
            for (ActivityAuditReportRow row : rows) {
                writer.writeNext(toCsvRow(row), false);
            }
        }
    }

    private void ensureHeadersWritten() throws IOException {
        if (headersWritten) {
            return;
        }

        try (ICSVWriter writer = createWriter()) {
            writer.writeNext(new String[] {"Activity Audit Report"}, false);
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

    private String[] toCsvRow(ActivityAuditReportRow row) {
        return new String[] {
            Objects.toString(row.getEventName(), ""),
            Objects.toString(row.getTableName(), ""),
            Objects.toString(row.getColumnName(), ""),
            Objects.toString(row.getOldValue(), ""),
            Objects.toString(row.getNewValue(), ""),
            formatDate(row.getCreatedDate()),
            Objects.toString(row.getUserName(), "")
        };
    }

    private String formatDate(LocalDate value) {
        return value == null ? "" : value.toString();
    }

    private void close() throws IOException {
        Files.deleteIfExists(file.toPath());
    }
}
