package uk.gov.hmcts.appregister.csds.ingress.audit;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import uk.gov.hmcts.appregister.audit.operation.AuditOperation;
import uk.gov.hmcts.appregister.common.enumeration.CrudEnum;

@RequiredArgsConstructor
@Getter
public enum CsdsIngestAuditOperation implements AuditOperation {
    MANUAL_CSDS_INGEST_AUDIT_EVENT("Manual CSDS Ingest", CrudEnum.CREATE),
    MANUAL_CSDS_TRIGGER_AUDIT_EVENT("Manual CSDS Trigger", CrudEnum.CREATE);

    private final String eventName;

    private final CrudEnum type;
}
