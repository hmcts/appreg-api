package uk.gov.hmcts.appregister.report.service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import uk.gov.hmcts.appregister.common.util.CsvUtil;
import uk.gov.hmcts.appregister.report.model.DurationReportRow;

class DurationReportLifecycle extends ReportCsvLifecycle<DurationReportRow> {
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

    DurationReportLifecycle() throws IOException {
        super("duration-report", "Duration Report", HEADERS);
    }

    @Override
    protected String[] toCsvRow(DurationReportRow row) {
        return new String[] {
            formatListDate(row.getListDate()),
            CsvUtil.escapeCharacters(Objects.toString(row.getCourthouseName(), "")),
            CsvUtil.escapeCharacters(Objects.toString(row.getOtherCourthouse(), "")),
            CsvUtil.escapeCharacters(Objects.toString(row.getCjaCode(), "")),
            CsvUtil.escapeCharacters(Objects.toString(row.getListDescription(), "")),
            CsvUtil.escapeCharacters(Objects.toString(row.getDurationHours(), "")),
            CsvUtil.escapeCharacters(Objects.toString(row.getDurationMinutes(), ""))
        };
    }

    private String formatListDate(LocalDate value) {
        return value == null ? "" : LIST_DATE_FORMAT.format(value);
    }
}
