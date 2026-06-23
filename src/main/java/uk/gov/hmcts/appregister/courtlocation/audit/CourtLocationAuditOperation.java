package uk.gov.hmcts.appregister.courtlocation.audit;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import uk.gov.hmcts.appregister.audit.operation.AuditOperation;
import uk.gov.hmcts.appregister.common.entity.TableNames;
import uk.gov.hmcts.appregister.common.enumeration.CrudEnum;

@RequiredArgsConstructor
@Getter
public enum CourtLocationAuditOperation implements AuditOperation {
    GET_COURT_LOCATION_AUDIT_EVENT("Get Court Location", CrudEnum.READ),
    GET_COURT_LOCATIONS_AUDIT_EVENT(TableNames.NATIONAL_COURT_HOUSES, CrudEnum.READ),
    CREATE_COURT_LOCATION_AUDIT_EVENT("Create Court Location", CrudEnum.CREATE),
    UPDATE_COURT_LOCATION_AUDIT_EVENT("Update Court Location", CrudEnum.UPDATE);

    private final String eventName;

    private final CrudEnum type;
}
