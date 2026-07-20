package uk.gov.hmcts.appregister.applicationlist.audit;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import uk.gov.hmcts.appregister.audit.listener.diff.Auditable;
import uk.gov.hmcts.appregister.audit.listener.diff.AuditableData;
import uk.gov.hmcts.appregister.common.entity.TableNames;
import uk.gov.hmcts.appregister.common.enumeration.CrudEnum;

@RequiredArgsConstructor
public class ApplicationListPrintReadAudit implements Auditable {
    private static final long PRINT_READ_AUDIT_ID = -1L;

    private final UUID listId;

    @Override
    public Long getId() {
        return PRINT_READ_AUDIT_ID;
    }

    @Override
    public List<AuditableData> extractAuditData(CrudEnum crudEnum) {
        return List.of(new AuditableData(TableNames.APPLICATION_LISTS, "id", listId.toString()));
    }
}
