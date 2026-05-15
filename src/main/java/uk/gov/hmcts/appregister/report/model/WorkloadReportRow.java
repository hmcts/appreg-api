package uk.gov.hmcts.appregister.report.model;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;

@Value
@Builder
public class WorkloadReportRow {
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
