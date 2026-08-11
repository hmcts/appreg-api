package uk.gov.hmcts.appregister.report.service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import uk.gov.hmcts.appregister.common.util.CsvUtil;
import uk.gov.hmcts.appregister.report.model.WorkloadReportRow;

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
        super("workload-report", "Workload Report", HEADERS);
    }

    @Override
    protected String[] toCsvRow(WorkloadReportRow row) {
        return new String[] {
            formatListDate(row.getListDate()),
            CsvUtil.escapeCharacters(Objects.toString(row.getListCourtHouseName(), "")),
            CsvUtil.escapeCharacters(Objects.toString(row.getListOtherLocation(), "")),
            CsvUtil.escapeCharacters(Objects.toString(row.getCjaCode(), "")),
            CsvUtil.escapeCharacters(Objects.toString(row.getListDescription(), "")),
            CsvUtil.escapeCharacters(Objects.toString(row.getStandardApplicantCode(), "")),
            CsvUtil.escapeCharacters(Objects.toString(row.getApplicantNameSurname(), "")),
            CsvUtil.escapeCharacters(Objects.toString(row.getApplicationCode(), "")),
            CsvUtil.escapeCharacters(Objects.toString(row.getApplicationCodeTitle(), "")),
            CsvUtil.escapeCharacters(Objects.toString(row.getResults(), "")),
            CsvUtil.escapeCharacters(Objects.toString(row.getJp1(), "")),
            CsvUtil.escapeCharacters(Objects.toString(row.getJp2(), "")),
            CsvUtil.escapeCharacters(Objects.toString(row.getJp3(), "")),
            CsvUtil.escapeCharacters(Objects.toString(row.getOfficial(), ""))
        };
    }

    private String formatListDate(LocalDate value) {
        return value == null ? "" : LIST_DATE_FORMAT.format(value);
    }
}
