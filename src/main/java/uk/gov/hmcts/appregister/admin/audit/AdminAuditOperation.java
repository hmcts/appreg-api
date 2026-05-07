package uk.gov.hmcts.appregister.admin.audit;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import uk.gov.hmcts.appregister.audit.operation.AuditOperation;
import uk.gov.hmcts.appregister.common.enumeration.CrudEnum;

@RequiredArgsConstructor
@Getter
public enum AdminAuditOperation implements AuditOperation {
    GET_DATABASE_JOB_STATUS_AUDIT_EVENT("Get Database Job Status", CrudEnum.READ);

    private final String eventName;

    private final CrudEnum type;
}
