package uk.gov.hmcts.appregister.report.service;

import uk.gov.hmcts.appregister.report.model.WorkloadReportRow;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

class WorkloadReportLifecycle extends ReportCsvLifecycle<WorkloadReportRow> {
    private static final DateTimeFormatter LIST_DATE_FORMAT =
        DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final String[] HEADERS = {
        "List Date",
        "List Court House Name",
        "List Other Location",
        "CJA Code",
        "List Description",
        "Standard Applicant Code",
        "Applicant Name/Surname",
        "Application Code",
        "Application Code Title",
        "Results",
        "JP1",
        "JP2",
        "JP3",
        "Official"
    };

    WorkloadReportLifecycle() throws IOException {
        super("workload-report", "Workload Report", headers);
    }

    @Override
    protected String[] toCsvRow(WorkloadReportRow row) {
        return new String[] {
            formatListDate(row.getListDate()),
            Objects.toString(row.getListCourtHouseName()),
            Objects.toString(row.getListOtherLocation()),
            Objects.toString(row.getCjaCode()),
            Objects.toString(row.getListDescription()),
            Objects.toString(row.getStandardApplicantCode()),
            Objects.toString(row.getApplicantNameSurname()),
            Objects.toString(row.getApplicationCode()),
            Objects.toString(row.getApplicationCodeTitle()),
            Objects.toString(row.getResults()),
            Objects.toString(row.getJp1()),
            Objects.toString(row.getJp2()),
            Objects.toString(row.getJp3()),
        Objects.toString(row.getOfficial())
        };
    }

    private String formatListDate(LocalDate value) {
        return value == null ? "" : LIST_DATE_FORMAT.format(value);
    }
}
