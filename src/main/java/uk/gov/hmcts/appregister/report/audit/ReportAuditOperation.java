package uk.gov.hmcts.appregister.report.audit;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import uk.gov.hmcts.appregister.common.audit.operation.AuditOperation;
import uk.gov.hmcts.appregister.common.enumeration.CrudEnum;

@RequiredArgsConstructor
@Getter
public enum ReportAuditOperation implements AuditOperation {
    CREATE_ACTIVITY_AUDIT_REPORT_AUDIT_EVENT("Create Activity Audit Report", CrudEnum.READ),
    CREATE_FEES_REPORT_AUDIT_EVENT("Create Fees Report", CrudEnum.READ),
    CREATE_DURATION_REPORT_AUDIT_EVENT("Create Duration Report", CrudEnum.READ),
    DOWNLOAD_REPORT_AUDIT_EVENT("Download Report", CrudEnum.READ);

    private final String eventName;

    private final CrudEnum type;
}
