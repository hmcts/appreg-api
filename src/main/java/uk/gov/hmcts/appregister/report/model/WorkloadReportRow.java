package uk.gov.hmcts.appregister.report.model;

import java.time.LocalDate;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class WorkloadReportRow {
    Long lastApplicationListEntryId;
    LocalDate listDate;
    String listCourtHouseName;
    String listOtherLocation;
    String cjaCode;
    String listDescription;
    String standardApplicantCode;
    String applicantNameSurname;
    String applicationCode;
    String applicationCodeTitle;
    String results;
    String jp1;
    String jp2;
    String jp3;
    String official;
}
