package uk.gov.hmcts.appregister.report.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ActivityAuditReportRow {
    Long dataId;
    Integer activityOrder;
    String eventName;
    String tableName;
    String columnName;
    String oldValue;
    String newValue;
    LocalDate createdDate;
    LocalDateTime createdDateTime;
    String userName;
}
