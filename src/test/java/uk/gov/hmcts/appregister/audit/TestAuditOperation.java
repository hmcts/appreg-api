package uk.gov.hmcts.appregister.audit;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import uk.gov.hmcts.appregister.audit.operation.AuditOperation;
import uk.gov.hmcts.appregister.common.entity.ApplicationCode;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.enumeration.CRUDEnum;

@RequiredArgsConstructor
@Getter
public enum TestAuditOperation implements AuditOperation<ApplicationList> {
    CREATE("Create Application List",CRUDEnum.CREATE, ApplicationList.class),
    UPDATE("Delete Application List",CRUDEnum.UPDATE, ApplicationList.class),
    DELETE("Create Application List",CRUDEnum.DELETE, ApplicationList.class),
    READ("Read Application List",CRUDEnum.READ, ApplicationList.class);

    private final String eventName;

    private final CRUDEnum type;

    private final Class<ApplicationList> entityClass;
}
