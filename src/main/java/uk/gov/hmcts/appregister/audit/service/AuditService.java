package uk.gov.hmcts.appregister.audit.service;

import uk.gov.hmcts.appregister.audit.AuditEnum;

/** An API that can be used to audit an event across all endpoints. */
public interface AuditService {
    /**
     * Records an audit event.
     *
     * @param auditEnum The audit enumeration entry to record
     */
    void record(AuditEnum auditEnum);
}
