package uk.gov.hmcts.appregister.courtlocation.audit;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import uk.gov.hmcts.appregister.audit.operation.AuditOperation;
import uk.gov.hmcts.appregister.common.entity.ApplicationCode;
import uk.gov.hmcts.appregister.common.entity.NationalCourtHouse;
import uk.gov.hmcts.appregister.common.entity.TableNames;
import uk.gov.hmcts.appregister.common.enumeration.CRUDEnum;

@RequiredArgsConstructor
@Getter
public enum CourtLocationAuditOperation implements AuditOperation<NationalCourtHouse> {

    GET_COURT_LOCATION_AUDIT_EVENT("Get Court Location",CRUDEnum.READ, NationalCourtHouse.class),
    GET_COURT_LOCATIONS_AUDIT_EVENT(TableNames.NATIONAL_COURT_HOUSES,  CRUDEnum.READ, NationalCourtHouse.class);

    private final String eventName;

    private final CRUDEnum type;

    private final Class<NationalCourtHouse> entityClass;
}