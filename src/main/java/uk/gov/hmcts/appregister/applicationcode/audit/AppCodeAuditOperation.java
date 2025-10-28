package uk.gov.hmcts.appregister.applicationcode.audit;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import uk.gov.hmcts.appregister.audit.operation.AuditOperation;
import uk.gov.hmcts.appregister.common.entity.ApplicationCode;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.enumeration.CRUDEnum;

@RequiredArgsConstructor
@Getter
public enum AppCodeAuditOperation implements AuditOperation<ApplicationCode> {
    GET_APPLICATION_CODE_AUDIT_EVENT("Get Application Code", CRUDEnum.READ, ApplicationCode.class),
    GET_APPLICATION_CODES_AUDIT_EVENT("Get Application Codes", CRUDEnum.READ, ApplicationCode.class);

    private final String eventName;

    private final CRUDEnum type;

    private final Class<ApplicationCode> entityClass;
}
