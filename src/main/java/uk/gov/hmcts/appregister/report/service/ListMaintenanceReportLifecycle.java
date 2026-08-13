package uk.gov.hmcts.appregister.report.service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import uk.gov.hmcts.appregister.common.util.CsvUtil;
import uk.gov.hmcts.appregister.report.model.ListMaintenanceReportRow;

class ListMaintenanceReportLifecycle extends ReportCsvLifecycle<ListMaintenanceReportRow> {
    private static final DateTimeFormatter LIST_DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String[] HEADERS = {
        "List Date",
        "List Court House Name",
        "List Other Location",
        "CJA Code",
        "List Description",
        "List Status",
        "No Of Application Entries"
    };

    ListMaintenanceReportLifecycle() throws IOException {
        super("list-maintenance-report", "List Maintenance Report", HEADERS);
    }

    @Override
    protected String[] toCsvRow(ListMaintenanceReportRow row) {
        return new String[] {
            formatListDate(row.getListDate()),
            CsvUtil.escapeCharacters(Objects.toString(row.getCourthouseName(), "")),
            CsvUtil.escapeCharacters(Objects.toString(row.getOtherCourthouse(), "")),
            CsvUtil.escapeCharacters(Objects.toString(row.getCjaCode(), "")),
            CsvUtil.escapeCharacters(Objects.toString(row.getListDescription(), "")),
            CsvUtil.escapeCharacters(Objects.toString(row.getListStatus(), "")),
            CsvUtil.escapeCharacters(Objects.toString(row.getApplicationEntryCount(), ""))
        };
    }

    private String formatListDate(LocalDate value) {
        return value == null ? "" : LIST_DATE_FORMAT.format(value);
    }
}
