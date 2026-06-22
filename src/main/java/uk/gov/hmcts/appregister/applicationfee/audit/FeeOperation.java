package uk.gov.hmcts.appregister.applicationfee.audit;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import uk.gov.hmcts.appregister.audit.operation.AuditOperation;
import uk.gov.hmcts.appregister.common.enumeration.CrudEnum;

@RequiredArgsConstructor
@Getter
public enum FeeOperation implements AuditOperation {
    CREATE_FEE("Create Fee", CrudEnum.CREATE),
    UPDATE_FEE("Update Fee", CrudEnum.UPDATE);

    private final String eventName;

    private final CrudEnum type;
}
