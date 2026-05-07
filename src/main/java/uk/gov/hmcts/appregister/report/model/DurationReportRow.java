package uk.gov.hmcts.appregister.report.model;

import java.time.LocalDate;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DurationReportRow {
    Long applicationListId;
    LocalDate listDate;
    String courthouseName;
    String otherCourthouse;
    String cjaCode;
    String listDescription;
    Integer durationHours;
    Integer durationMinutes;
}
