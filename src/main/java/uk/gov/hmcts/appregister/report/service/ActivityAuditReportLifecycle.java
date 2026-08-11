package uk.gov.hmcts.appregister.report.service;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Objects;
import uk.gov.hmcts.appregister.common.util.CsvUtil;
import uk.gov.hmcts.appregister.report.model.ActivityAuditReportRow;

class ActivityAuditReportLifecycle extends ReportCsvLifecycle<ActivityAuditReportRow> {
    private static final String[] HEADERS = {
        "Event Name",
        "Table Name",
        "Column Name",
        "Old Value",
        "New Value",
        "Created Date",
        "User Name"
    };

    ActivityAuditReportLifecycle() throws IOException {
        super("activity-audit-report", "Activity Audit Report", HEADERS);
    }

    @Override
    protected String[] toCsvRow(ActivityAuditReportRow row) {
        return new String[] {
            CsvUtil.escapeCharacters(Objects.toString(row.getEventName(), "")),
            CsvUtil.escapeCharacters(Objects.toString(row.getTableName(), "")),
            CsvUtil.escapeCharacters(Objects.toString(row.getColumnName(), "")),
            CsvUtil.escapeCharacters(Objects.toString(row.getOldValue(), "")),
            CsvUtil.escapeCharacters(Objects.toString(row.getNewValue(), "")),
            formatDate(row.getCreatedDate()),
            CsvUtil.escapeCharacters(Objects.toString(row.getUserName(), ""))
        };
    }

    private String formatDate(LocalDate value) {
        return value == null ? "" : value.toString();
    }
}
