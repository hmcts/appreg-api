package uk.gov.hmcts.appregister.audit.listener.diff;

import java.util.List;
import uk.gov.hmcts.appregister.common.entity.base.Keyable;
import uk.gov.hmcts.appregister.common.enumeration.CrudEnum;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.exception.CommonAppError;

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
        if (!(this instanceof AuditableData)) {
            throw new AppRegistryException(
                    CommonAppError.INTERNAL_SERVER_ERROR, "Usage error. Cant establish diff");
        }
        return ReflectiveAuditor.extractAuditData(crudEnum, this, true);
    }
}
