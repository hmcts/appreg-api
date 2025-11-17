package uk.gov.hmcts.appregister.applicationcode.audit;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import uk.gov.hmcts.appregister.audit.operation.AuditOperation;
import uk.gov.hmcts.appregister.common.enumeration.CrudEnum;

@RequiredArgsConstructor
@Getter
public enum AppCodeAuditOperation implements AuditOperation {
    GET_APPLICATION_CODE_AUDIT_EVENT("Get Application Code", CrudEnum.READ),
    GET_APPLICATION_CODES_AUDIT_EVENT("Get Application Codes", CrudEnum.READ);

    private final String eventName;

    private final CrudEnum type;
}
