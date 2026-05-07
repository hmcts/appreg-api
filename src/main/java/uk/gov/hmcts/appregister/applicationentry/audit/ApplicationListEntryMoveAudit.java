package uk.gov.hmcts.appregister.applicationentry.audit;

import java.util.List;
import lombok.RequiredArgsConstructor;
import uk.gov.hmcts.appregister.audit.listener.diff.Auditable;
import uk.gov.hmcts.appregister.audit.listener.diff.AuditableData;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.TableNames;
import uk.gov.hmcts.appregister.common.enumeration.CrudEnum;

@RequiredArgsConstructor
public class ApplicationListEntryMoveAudit implements Auditable {

    private final Long entryId;
    private final Long listId;
    private final Long version;

    public static ApplicationListEntryMoveAudit from(ApplicationListEntry entry) {
        return new ApplicationListEntryMoveAudit(
                entry.getId(),
                entry.getApplicationList() != null ? entry.getApplicationList().getId() : null,
                entry.getVersion());
    }

    @Override
    public Long getId() {
        return entryId;
    }

    @Override
    public List<AuditableData> extractAuditData(CrudEnum crudEnum) {
        return List.of(
                new AuditableData(
                        TableNames.APPLICATION_LISTS,
                        "al_id",
                        listId != null ? listId.toString() : ""),
                new AuditableData(
                        TableNames.APPLICATION_LISTS_ENTRY,
                        "version",
                        version != null ? version.toString() : ""));
    }
}
