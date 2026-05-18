package uk.gov.hmcts.appregister.report.audit;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import uk.gov.hmcts.appregister.audit.operation.AuditOperation;
import uk.gov.hmcts.appregister.common.enumeration.CrudEnum;

@RequiredArgsConstructor
@Getter
public enum ReportAuditOperation implements AuditOperation {
    CREATE_ACTIVITY_AUDIT_REPORT_AUDIT_EVENT("Create Activity Audit Report", CrudEnum.CREATE),
    CREATE_FEES_REPORT_AUDIT_EVENT("Create Fees Report", CrudEnum.CREATE),
    CREATE_DURATION_REPORT_AUDIT_EVENT("Create Duration Report", CrudEnum.CREATE),
    CREATE_WORKLOAD_REPORT_AUDIT_EVENT("Create Workload Report", CrudEnum.READ),
    CREATE_LIST_MAINTENANCE_REPORT_AUDIT_EVENT("Create List Maintenance Report", CrudEnum.CREATE),
    CREATE_PRIVATE_PROSECUTORS_INDEX_REPORT_AUDIT_EVENT(
            "Create Private Prosecutors Index Report", CrudEnum.CREATE),
    REPORT_JOB_STATUS_TRANSITION_AUDIT_EVENT("Report Job Status Transition", CrudEnum.UPDATE),
    DOWNLOAD_REPORT_AUDIT_EVENT("Download Report", CrudEnum.READ);

    private final String eventName;

    private final CrudEnum type;
}
