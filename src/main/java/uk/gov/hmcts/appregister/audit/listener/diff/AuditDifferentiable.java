package uk.gov.hmcts.appregister.audit.listener.diff;

import uk.gov.hmcts.appregister.common.entity.base.Keyable;
import uk.gov.hmcts.appregister.common.enumeration.CRUDEnum;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.exception.CommonAppError;

import java.util.List;

/**
 * Very similar to a @{link Comparable}, but for diffing purposes.
 */
public interface AuditDifferentiable extends Keyable {
    /**
     * establish the difference between this object and t
     * @param crudEnum The audit CRUD operation being performed
     * @param t The other object to diff against
     * @return The list of differences that exist at the field level
     */
    default List<Difference> diff(CRUDEnum crudEnum, Keyable t) {
        if (!(t instanceof Difference)) {
            throw new AppRegistryException(CommonAppError.INTERNAL_SERVER_ERROR, "Usage error. Cant establish diff");
        }
        return  ReflectiveAuditDifferentiator.difference(crudEnum, t,  this, false, false);
    }
}
