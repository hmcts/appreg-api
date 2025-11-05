package uk.gov.hmcts.appregister.applicationlist.audit;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import uk.gov.hmcts.appregister.audit.operation.AuditOperation;
import uk.gov.hmcts.appregister.common.enumeration.CrudEnum;

@RequiredArgsConstructor
@Getter
public enum AppListAuditOperation implements AuditOperation {
    CREATE_APP_LIST("Create Application List", CrudEnum.CREATE);
    private final String eventName;

    private final CrudEnum type;
}
