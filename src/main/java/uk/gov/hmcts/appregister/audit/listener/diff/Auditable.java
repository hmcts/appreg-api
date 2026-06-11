package uk.gov.hmcts.appregister.audit.listener.diff;

import java.util.List;
import uk.gov.hmcts.appregister.common.entity.base.Keyable;
import uk.gov.hmcts.appregister.common.enumeration.CrudEnum;

/**
 * Very similar to a @{link Comparable}, but for audit diffing purposes.
 */
public interface Auditable extends Keyable {
    /**
     * establish the audit difference for this object.
     *
     * @param crudEnum The audit CRUD operation being performed
     * @return The list of differences that exist at the field level
     */
    default List<AuditableData> extractAuditData(CrudEnum crudEnum) {
        return ReflectiveAuditor.extractAuditData(crudEnum, this, true);
    }
}
