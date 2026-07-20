package uk.gov.hmcts.appregister.applicationentry.audit;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import uk.gov.hmcts.appregister.audit.operation.AuditOperation;
import uk.gov.hmcts.appregister.common.enumeration.CrudEnum;

@RequiredArgsConstructor
@Getter
public enum AppListEntryAuditOperation implements AuditOperation {
    CREATE_APP_ENTRY_LIST("Create Entry Application List", CrudEnum.CREATE),
    BULK_IMPORT_APP_ENTRIES("Bulk Import Application List Entries", CrudEnum.CREATE),
    BULK_UPDATE_OFFICIALS("Bulk Update Officials", CrudEnum.UPDATE),
    UPDATE_APP_ENTRY_LIST("Update Entry Application List", CrudEnum.UPDATE),
    UPDATE_CLOSED_APP_ENTRY_LIST("Update Closed Entry Application List", CrudEnum.UPDATE),
    CREATE_OFFICIAL_ENTRY("Create Official", CrudEnum.CREATE),
    DELETE_OFFICIAL_ENTRY("Delete Official", CrudEnum.DELETE),
    CREATE_FEE_STATUS_ENTRY("Create Fee Status", CrudEnum.CREATE),
    DELETE_FEE_STATUS_ENTRY("Delete Fee Status", CrudEnum.DELETE),
    CREATE_FEE_ENTRY("Create Fee to Entry", CrudEnum.CREATE),
    DELETE_FEE_ENTRY("Delete Fee to Entry", CrudEnum.DELETE),
    CREATE_APPLICANT("Create Applicant", CrudEnum.CREATE),
    DELETE_APPLICANT("Delete Applicant", CrudEnum.DELETE),
    CREATE_RESPONDENT("Create Respondent", CrudEnum.CREATE),
    DELETE_RESPONDENT("Delete Respondent", CrudEnum.DELETE),
    MOVE_APP_ENTRY("Move Entry", CrudEnum.UPDATE),
    GET_APP_ENTRY_LIST_DETAIL("Get Entry Application List Detail", CrudEnum.READ),
    SEARCH_APP_ENTRY_LIST("Search Entry Application List", CrudEnum.READ),
    DELETE_ENTRY("Delete Entry", CrudEnum.DELETE);
    private final String eventName;

    private final CrudEnum type;
}
