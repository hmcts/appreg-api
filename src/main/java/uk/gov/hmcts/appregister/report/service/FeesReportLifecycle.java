package uk.gov.hmcts.appregister.report.service;

import com.opencsv.CSVWriterBuilder;
import com.opencsv.ICSVWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
import uk.gov.hmcts.appregister.report.model.FeesReportRow;

class FeesReportLifecycle implements AsyncJobLifecycle<FeesReportRow> {
    private static final DateTimeFormatter LIST_DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String[] HEADERS = {
        "List Date",
        "List Court House Name",
        "List Other Location",
        "CJA Code",
        "Standard Applicant Code",
        "Applicant Name/Surname",
        "Application Code",
        "Application Code Title",
        "Fee Value",
        "Off Site Fee Value",
        "Total fee Value",
        "Fee Status",
        "Fee Status Date",
        "Payment Reference"
    };

    private final File file;
    private boolean headersWritten;

    FeesReportLifecycle() throws IOException {
        this.file = AppRegTempFileUtil.generateTempFile();
    }

    @Override
    public void processing(AsyncJobLifecycleEvent<FeesReportRow> event) throws IOException {
        writeRows(event.getData());
    }

    @Override
    public void completed(AsyncJobLifecycleEvent<FeesReportRow> event) throws IOException {
        ensureHeadersWritten();

        try (FileInputStream inputStream = new FileInputStream(file)) {
            event.getResponse().write(inputStream);
        } finally {
            close();
        }
    }

    @Override
    public void failed(AsyncJobLifecycleEvent<FeesReportRow> event) throws IOException {
        close();
    }

    private void writeRows(List<FeesReportRow> rows) throws IOException {
        ensureHeadersWritten();

        try (ICSVWriter writer = createWriter()) {
            for (FeesReportRow row : rows) {
                writer.writeNext(toCsvRow(row), false);
            }
        }
    }

    private void ensureHeadersWritten() throws IOException {
        if (headersWritten) {
            return;
        }

        try (ICSVWriter writer = createWriter()) {
            writer.writeNext(new String[] {"Fees Report"}, false);
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

    private String[] toCsvRow(FeesReportRow row) {
        return new String[] {
            formatListDate(row.getListDate()),
            Objects.toString(row.getCourthouseName(), ""),
            Objects.toString(row.getOtherCourthouse(), ""),
            Objects.toString(row.getCjaCode(), ""),
            Objects.toString(row.getStandardApplicantCode(), ""),
            Objects.toString(row.getApplicantFullName(), ""),
            Objects.toString(row.getApplicationCode(), ""),
            Objects.toString(row.getApplicationCodeTitle(), ""),
            formatMoney(row.getFeeValue()),
            formatMoney(row.getOffSiteFeeValue()),
            formatMoney(row.getTotalFeeValue()),
            Objects.toString(row.getFeeStatus(), ""),
            formatIsoDate(row.getFeeStatusDate()),
            Objects.toString(row.getPaymentReference(), "")
        };
    }

    private String formatListDate(LocalDate value) {
        return value == null ? "" : LIST_DATE_FORMAT.format(value);
    }

    private String formatIsoDate(LocalDate value) {
        return value == null ? "" : value.toString();
    }

    private String formatMoney(BigDecimal value) {
        return value == null ? "" : value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private void close() throws IOException {
        Files.deleteIfExists(file.toPath());
    }
}
