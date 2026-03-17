package uk.gov.hmcts.appregister.applicationentryresult.audit;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import uk.gov.hmcts.appregister.common.audit.operation.AuditOperation;
import uk.gov.hmcts.appregister.common.enumeration.CrudEnum;

@RequiredArgsConstructor
@Getter
public enum AppListEntryResultAuditOperation implements AuditOperation {
    DELETE_APP_LIST_ENTRY_RESULT("Delete Application List Entry Result", CrudEnum.DELETE),
    CREATE_APP_LIST_ENTRY_RESULT("Create Application List Entry Result", CrudEnum.CREATE),
    UPDATE_APP_LIST_ENTRY_RESULT("Update Application List Entry Result", CrudEnum.UPDATE);
    private final String eventName;

    private final CrudEnum type;
}
