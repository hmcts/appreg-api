package uk.gov.hmcts.appregister.applicationlist.audit;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import uk.gov.hmcts.appregister.common.audit.operation.AuditOperation;
import uk.gov.hmcts.appregister.common.enumeration.CrudEnum;

@RequiredArgsConstructor
@Getter
public enum AppListAuditOperation implements AuditOperation {
    CREATE_APP_LIST("Create Application List", CrudEnum.CREATE),
    UPDATE_APP_LIST("Update Application List", CrudEnum.UPDATE),
    DELETE_APP_LIST("Delete Application List", CrudEnum.DELETE),
    GET_APP_LIST("Get Application List", CrudEnum.READ),
    PRINT_APP_LIST("Print Application List", CrudEnum.READ);

    private final String eventName;

    private final CrudEnum type;
}
