package uk.gov.hmcts.appregister.applicationlist.audit;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import uk.gov.hmcts.appregister.audit.operation.AuditOperation;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.enumeration.CRUDEnum;

@RequiredArgsConstructor
@Getter
public enum AppListAuditOperation implements AuditOperation<ApplicationList> {
    CREATE_APP_LIST("Create Application List",CRUDEnum.CREATE, ApplicationList.class);

    private final String eventName;

    private final CRUDEnum type;

    private final Class<ApplicationList> entityClass;

}



