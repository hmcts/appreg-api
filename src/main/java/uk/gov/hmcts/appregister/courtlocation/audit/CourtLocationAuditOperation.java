package uk.gov.hmcts.appregister.courtlocation.audit;

import lombok.RequiredArgsConstructor;
import uk.gov.hmcts.appregister.audit.operation.AuditOperation;
import uk.gov.hmcts.appregister.common.entity.TableNames;
import uk.gov.hmcts.appregister.common.enumeration.CRUDEnum;

@RequiredArgsConstructor
public enum CourtLocationAuditOperation implements AuditOperation {

    GET_COURT_LOCATION_AUDIT_EVENT("Get Court Location",CRUDEnum.READ),
    GET_COURT_LOCATIONS_AUDIT_EVENT(TableNames.NATIONAL_COURT_HOUSES,  CRUDEnum.READ);

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