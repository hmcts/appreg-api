package uk.gov.hmcts.appregister.applicationentry.audit;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import uk.gov.hmcts.appregister.audit.listener.diff.Auditable;
import uk.gov.hmcts.appregister.audit.listener.diff.AuditableData;
import uk.gov.hmcts.appregister.common.entity.TableNames;
import uk.gov.hmcts.appregister.common.enumeration.CrudEnum;

@RequiredArgsConstructor
public class BulkApplicationListEntriesReadAudit implements Auditable {
    private static final long BULK_READ_AUDIT_ID = -1L;

    private final List<UUID> listIds;
    private final List<UUID> entryIds;

    @Override
    public Long getId() {
        return BULK_READ_AUDIT_ID;
    }

    @Override
    public List<AuditableData> extractAuditData(CrudEnum crudEnum) {
        var auditData = new ArrayList<AuditableData>();
        auditData.add(new AuditableData(TableNames.APPLICATION_LISTS, "id", joinIds(listIds)));

        if (entryIds != null && !entryIds.isEmpty()) {
            auditData.add(
                    new AuditableData(TableNames.APPLICATION_LISTS_ENTRY, "id", joinIds(entryIds)));
        }

        return auditData;
    }

    private String joinIds(List<UUID> ids) {
        return ids.stream().map(UUID::toString).toList().toString();
    }
}
