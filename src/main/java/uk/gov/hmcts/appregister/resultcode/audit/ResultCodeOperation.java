package uk.gov.hmcts.appregister.resultcode.audit;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import uk.gov.hmcts.appregister.audit.operation.AuditOperation;
import uk.gov.hmcts.appregister.common.enumeration.CrudEnum;

@RequiredArgsConstructor
@Getter
public enum ResultCodeOperation implements AuditOperation {
    GET_RESULT_CODE_AUDIT_EVENT("Get Result Code", CrudEnum.READ),
    GET_RESULT_CODES_AUDIT_EVENT("Get Result Codes", CrudEnum.READ);

    private final String eventName;

    private final CrudEnum type;
}
