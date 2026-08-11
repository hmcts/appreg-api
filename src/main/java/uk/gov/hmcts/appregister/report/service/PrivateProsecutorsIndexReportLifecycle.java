package uk.gov.hmcts.appregister.report.service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import uk.gov.hmcts.appregister.common.util.CsvUtil;
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
            CsvUtil.escapeCharacters(Objects.toString(row.getCourthouseName(), "")),
            CsvUtil.escapeCharacters(Objects.toString(row.getOtherCourthouse(), "")),
            CsvUtil.escapeCharacters(Objects.toString(row.getCjaCode(), "")),
            CsvUtil.escapeCharacters(Objects.toString(row.getApplicantNameOrSurname(), "")),
            CsvUtil.escapeCharacters(Objects.toString(row.getApplicantFirstName(), "")),
            CsvUtil.escapeCharacters(Objects.toString(row.getStandardApplicantName(), "")),
            CsvUtil.escapeCharacters(Objects.toString(row.getRespondentFirstName(), "")),
            CsvUtil.escapeCharacters(Objects.toString(row.getRespondentSurname(), "")),
            CsvUtil.escapeCharacters(Objects.toString(row.getRespondentOrganisationName(), "")),
            CsvUtil.escapeCharacters(Objects.toString(row.getApplicationWording(), "")),
            CsvUtil.escapeCharacters(Objects.toString(row.getResult1(), "")),
            CsvUtil.escapeCharacters(Objects.toString(row.getResult2(), "")),
            CsvUtil.escapeCharacters(Objects.toString(row.getResult3(), "")),
            CsvUtil.escapeCharacters(Objects.toString(row.getResult4(), "")),
            CsvUtil.escapeCharacters(Objects.toString(row.getNotes(), ""))
        };
    }

    private String formatListDate(LocalDate value) {
        return value == null ? "" : LIST_DATE_FORMAT.format(value);
    }
}
