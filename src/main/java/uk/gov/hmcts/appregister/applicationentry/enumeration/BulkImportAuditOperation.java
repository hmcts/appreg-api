package uk.gov.hmcts.appregister.applicationentry.enumeration;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import uk.gov.hmcts.appregister.audit.operation.AuditOperation;
import uk.gov.hmcts.appregister.common.enumeration.CrudEnum;

/**
 * Audit operations emitted during the bulk import lifecycle for application list entries.
 */
@Getter
@RequiredArgsConstructor
public enum BulkImportAuditOperation implements AuditOperation {
    BULK_IMPORT_START("Bulk Import Start", CrudEnum.CREATE),
    BULK_IMPORT_COMPLETE("Bulk Import Complete", CrudEnum.CREATE),
    BULK_IMPORT_VALIDATE_ROW("Bulk Import Validate Row", CrudEnum.READ),
    BULK_IMPORT_CREATE_ENTRY("Bulk Import Create Entry", CrudEnum.CREATE);

    private final String eventName;
    private final CrudEnum type;
}
