package uk.gov.hmcts.appregister.criminaljusticearea.audit;

import lombok.RequiredArgsConstructor;
import uk.gov.hmcts.appregister.audit.operation.AuditOperation;
import uk.gov.hmcts.appregister.common.enumeration.CRUDEnum;

@RequiredArgsConstructor
public enum CriminalJusticeAuditOperation implements AuditOperation {
    GET_CRIMINAL_JUSTICE_AUDIT_EVENT( "Get Court Justice Area",CRUDEnum.READ),

    GET_CRIMINAL_JUSTICE_AUDITS_EVENT("Get Court Justice Areaa", CRUDEnum.READ);

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
