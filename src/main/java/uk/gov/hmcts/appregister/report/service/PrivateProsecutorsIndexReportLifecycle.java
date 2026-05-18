package uk.gov.hmcts.appregister.report.service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import uk.gov.hmcts.appregister.report.model.PrivateProsecutorsIndexReportRow;

class PrivateProsecutorsIndexReportLifecycle
        extends ReportCsvLifecycle<PrivateProsecutorsIndexReportRow> {
    private static final DateTimeFormatter LIST_DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String[] HEADERS = {
        "List Date",
        "List Court House Name",
        "List Other Location",
        "CJA Code",
        "Applicant Name/Surname",
        "Applicant First Name",
        "Standard Applicant Name",
        "Respondent First Name",
        "Respondent Surname",
        "Respondent Organisation Name",
        "Application Wording",
        "Result 1",
        "Result 2",
        "Result 3",
        "Result 4",
        "Application Notes"
    };

    PrivateProsecutorsIndexReportLifecycle() throws IOException {
        super("private-prosecutors-index-report", "Private Prosecution Index Report", HEADERS);
    }

    @Override
    protected String[] toCsvRow(PrivateProsecutorsIndexReportRow row) {
        return new String[] {
            formatListDate(row.getListDate()),
            Objects.toString(row.getCourthouseName(), ""),
            Objects.toString(row.getOtherCourthouse(), ""),
            Objects.toString(row.getCjaCode(), ""),
            Objects.toString(row.getApplicantNameOrSurname(), ""),
            Objects.toString(row.getApplicantFirstName(), ""),
            Objects.toString(row.getStandardApplicantName(), ""),
            Objects.toString(row.getRespondentFirstName(), ""),
            Objects.toString(row.getRespondentSurname(), ""),
            Objects.toString(row.getRespondentOrganisationName(), ""),
            Objects.toString(row.getApplicationWording(), ""),
            Objects.toString(row.getResult1(), ""),
            Objects.toString(row.getResult2(), ""),
            Objects.toString(row.getResult3(), ""),
            Objects.toString(row.getResult4(), ""),
            Objects.toString(row.getNotes(), "")
        };
    }

    private String formatListDate(LocalDate value) {
        return value == null ? "" : LIST_DATE_FORMAT.format(value);
    }
}
