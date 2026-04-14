package uk.gov.hmcts.appregister.audit;

import uk.gov.hmcts.appregister.audit.operation.AuditOperation;
import uk.gov.hmcts.appregister.common.enumeration.CrudEnum;

public enum TestAuditOperation implements AuditOperation {
    CREATE("Create Application List", CrudEnum.CREATE),
    UPDATE("Update Application List", CrudEnum.UPDATE),
    DELETE("Delete Application List", CrudEnum.DELETE),
    READ("Read Application List", CrudEnum.READ);

    private final String eventName;

    private final CrudEnum type;

    TestAuditOperation(String eventName, CrudEnum type) {
        this.eventName = eventName;
        this.type = type;
    }

    @Override
    public String getEventName() {
        return eventName;
    }

    @Override
    public CrudEnum getType() {
        return type;
    }
}
