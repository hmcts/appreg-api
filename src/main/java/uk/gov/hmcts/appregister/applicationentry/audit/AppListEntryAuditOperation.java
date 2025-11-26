package uk.gov.hmcts.appregister.applicationentry.audit;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import uk.gov.hmcts.appregister.audit.operation.AuditOperation;
import uk.gov.hmcts.appregister.common.enumeration.CrudEnum;

@RequiredArgsConstructor
@Getter
public enum AppListEntryAuditOperation implements AuditOperation {
    CREATE_APP_ENTRY_LIST("Create Entry Application List",CrudEnum.CREATE);
    private final String eventName;

    private final CrudEnum type;
}
