package uk.gov.hmcts.appregister.applicationentryresult.audit;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import uk.gov.hmcts.appregister.audit.operation.AuditOperation;
import uk.gov.hmcts.appregister.common.enumeration.CrudEnum;

@RequiredArgsConstructor
@Getter
public enum AppListEntryResultAuditOperation implements AuditOperation {
    DELETE_APP_LIST_ENTRY_RESULT("Delete Application List Entry Result", CrudEnum.DELETE);
    private final String eventName;

    private final CrudEnum type;
}
