package uk.gov.hmcts.appregister.criminaljusticearea.audit;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import uk.gov.hmcts.appregister.audit.operation.AuditOperation;
import uk.gov.hmcts.appregister.common.enumeration.CrudEnum;

@RequiredArgsConstructor
@Getter
public enum CriminalJusticeAuditOperation implements AuditOperation {
    GET_CRIMINAL_JUSTICE_AUDIT_EVENT("Get Court Justice Area", CrudEnum.READ),
    GET_CRIMINAL_JUSTICE_AUDITS_EVENT("Get Court Justice Areaa", CrudEnum.READ);

    private final String eventName;

    private final CrudEnum type;
}
