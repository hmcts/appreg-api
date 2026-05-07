package uk.gov.hmcts.appregister.job.audit;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import uk.gov.hmcts.appregister.audit.operation.AuditOperation;
import uk.gov.hmcts.appregister.common.enumeration.CrudEnum;

@Getter
@RequiredArgsConstructor
public enum JobAuditOperation implements AuditOperation {
    GET_JOB_STATUS_AUDIT_EVENT("Get Job Status", CrudEnum.READ);

    private final String eventName;

    private final CrudEnum type;
}
