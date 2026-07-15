package uk.gov.hmcts.appregister.applicationentry.audit;

import java.util.List;
import java.util.UUID;
import uk.gov.hmcts.appregister.audit.listener.diff.Auditable;
import uk.gov.hmcts.appregister.audit.listener.diff.AuditableData;
import uk.gov.hmcts.appregister.common.entity.TableNames;
import uk.gov.hmcts.appregister.common.enumeration.CrudEnum;

/**
 * Logical audit data for one successfully completed application-entry bulk import.
 */
public record BulkImportAudit(UUID applicationListId, UUID jobId, int importedEntryCount)
        implements Auditable {

    @Override
    public Long getId() {
        return null;
    }

    @Override
    public List<AuditableData> extractAuditData(CrudEnum crudEnum) {
        return List.of(
                new AuditableData(
                        TableNames.APPLICATION_LISTS_ENTRY,
                        "application_list_id",
                        applicationListId.toString()),
                new AuditableData(
                        TableNames.APPLICATION_LISTS_ENTRY, "bulk_import_job_id", jobId.toString()),
                new AuditableData(
                        TableNames.APPLICATION_LISTS_ENTRY,
                        "imported_entry_count",
                        Integer.toString(importedEntryCount)));
    }
}
