package uk.gov.hmcts.appregister.applicationcode.audit;

import lombok.RequiredArgsConstructor;
import uk.gov.hmcts.appregister.audit.operation.AuditOperation;
import uk.gov.hmcts.appregister.common.enumeration.CRUDEnum;

@RequiredArgsConstructor
public enum AppCodeAuditOperation implements AuditOperation {
    GET_APPLICATION_CODE_AUDIT_EVENT("Get Application Code", CRUDEnum.READ),

    GET_APPLICATION_CODES_AUDIT_EVENT("Get Application Codes", CRUDEnum.READ);

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
