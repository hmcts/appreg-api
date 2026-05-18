package uk.gov.hmcts.appregister.report.model;

import java.time.LocalDate;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PrivateProsecutorsIndexReportRow {
    Long applicationListEntryId;
    LocalDate listDate;
    String courthouseName;
    String otherCourthouse;
    String cjaCode;
    String applicantNameOrSurname;
    String applicantFirstName;
    String standardApplicantName;
    String respondentFirstName;
    String respondentSurname;
    String respondentOrganisationName;
    String applicationWording;
    String result1;
    String result2;
    String result3;
    String result4;
    String notes;
}
