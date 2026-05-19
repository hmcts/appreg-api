package uk.gov.hmcts.appregister.report.service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import uk.gov.hmcts.appregister.report.model.SearchWarrantsReportRow;

class SearchWarrantsReportLifecycle extends ReportCsvLifecycle<SearchWarrantsReportRow> {
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
        "Application Code Wording"
    };

    SearchWarrantsReportLifecycle() throws IOException {
        super("search-warrants-report", "Search Warrants Report", HEADERS);
    }

    @Override
    protected String[] toCsvRow(SearchWarrantsReportRow row) {
        return new String[] {
            formatListDate(row.getListDate()),
            Objects.toString(row.getCourthouseName(), ""),
            Objects.toString(row.getOtherCourthouse(), ""),
            Objects.toString(row.getCjaCode(), ""),
            Objects.toString(row.getStandardApplicantCode(), ""),
            Objects.toString(row.getApplicantFullName(), ""),
            Objects.toString(row.getApplicationCode(), ""),
            Objects.toString(row.getApplicationCodeWording(), "")
        };
    }

    private String formatListDate(LocalDate value) {
        return value == null ? "" : LIST_DATE_FORMAT.format(value);
    }
}
