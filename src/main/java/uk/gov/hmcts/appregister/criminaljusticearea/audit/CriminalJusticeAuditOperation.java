package uk.gov.hmcts.appregister.criminaljusticearea.audit;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import uk.gov.hmcts.appregister.audit.operation.AuditOperation;
import uk.gov.hmcts.appregister.common.entity.CriminalJusticeArea;
import uk.gov.hmcts.appregister.common.entity.NationalCourtHouse;
import uk.gov.hmcts.appregister.common.enumeration.CRUDEnum;

@RequiredArgsConstructor
@Getter
public enum CriminalJusticeAuditOperation implements AuditOperation<CriminalJusticeArea> {
    GET_CRIMINAL_JUSTICE_AUDIT_EVENT( "Get Court Justice Area",CRUDEnum.READ, CriminalJusticeArea.class),
    GET_CRIMINAL_JUSTICE_AUDITS_EVENT("Get Court Justice Areaa", CRUDEnum.READ, CriminalJusticeArea.class);

    private final String eventName;

    private final CRUDEnum type;

    private final Class<CriminalJusticeArea> entityClass;
}
