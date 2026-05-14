package uk.gov.hmcts.appregister.report.model;

import java.time.LocalDate;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class ListMaintenanceReportRow {
    private final Long applicationListId;
    private final LocalDate listDate;
    private final String courthouseName;
    private final String otherCourthouse;
    private final String cjaCode;
    private final String listDescription;
    private final String listStatus;
    private final Long applicationEntryCount;
}
