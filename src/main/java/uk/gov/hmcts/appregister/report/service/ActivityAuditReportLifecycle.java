package uk.gov.hmcts.appregister.report.service;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Objects;
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
}
