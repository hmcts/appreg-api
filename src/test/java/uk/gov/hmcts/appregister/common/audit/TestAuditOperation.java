package uk.gov.hmcts.appregister.common.audit;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import uk.gov.hmcts.appregister.common.audit.operation.AuditOperation;
import uk.gov.hmcts.appregister.common.enumeration.CrudEnum;

@RequiredArgsConstructor
@Getter
public enum TestAuditOperation implements AuditOperation {
    CREATE("Create Application List", CrudEnum.CREATE),
    UPDATE("Update Application List", CrudEnum.UPDATE),
    DELETE("Delete Application List", CrudEnum.DELETE),
    READ("Read Application List", CrudEnum.READ);

    private final String eventName;

    private final CrudEnum type;
}
