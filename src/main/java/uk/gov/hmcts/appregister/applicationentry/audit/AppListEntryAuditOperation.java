package uk.gov.hmcts.appregister.applicationentry.audit;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import uk.gov.hmcts.appregister.audit.operation.AuditOperation;
import uk.gov.hmcts.appregister.common.enumeration.CrudEnum;

@RequiredArgsConstructor
@Getter
public enum AppListEntryAuditOperation implements AuditOperation {
    CREATE_APP_LIST_ENTRY("CREATE_APP_LIST_ENTRY", CrudEnum.CREATE),
    CREATE_OFFICIAL_ENTRY("Create Official", CrudEnum.CREATE),
    CREATE_FEE_STATUS_ENTRY("Create Fee Status Official", CrudEnum.CREATE),
    CREATE_FEE_ENTRY("Create Fee to Entry", CrudEnum.CREATE),
    CREATE_APPLICANT("Create Applicant", CrudEnum.CREATE),
    CREATE_RESPONDENT("Create Respondent", CrudEnum.CREATE);
    private final String eventName;

    private final CrudEnum type;
}
