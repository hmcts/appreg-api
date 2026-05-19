package uk.gov.hmcts.appregister.report.model;

import java.time.LocalDate;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SearchWarrantsReportRow {
    Long applicationListEntryId;
    LocalDate listDate;
    String courthouseName;
    String otherCourthouse;
    String cjaCode;
    String standardApplicantCode;
    String applicantFullName;
    String applicationCode;
    String applicationCodeWording;
}
