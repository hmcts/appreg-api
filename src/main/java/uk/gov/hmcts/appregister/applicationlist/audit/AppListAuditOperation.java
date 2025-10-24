package uk.gov.hmcts.appregister.applicationlist.audit;

import lombok.RequiredArgsConstructor;
import uk.gov.hmcts.appregister.audit.operation.AuditOperation;
import uk.gov.hmcts.appregister.common.enumeration.CRUDEnum;

@RequiredArgsConstructor
public enum AppListAuditOperation implements AuditOperation {
    CREATE_APP_LIST("Create Application List",CRUDEnum.CREATE);

    private final String eventName;

    private final CRUDEnum crudEnum;

    @Override
    public String getEventName() {
        return eventName;
    }

    @Override
    public CRUDEnum getType() {
        return crudEnum;
    }
}



