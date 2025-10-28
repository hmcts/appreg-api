package uk.gov.hmcts.appregister.audit.listener.diff;

import uk.gov.hmcts.appregister.common.entity.base.Keyable;
import uk.gov.hmcts.appregister.common.enumeration.CRUDEnum;

import java.util.List;

/**
 * An interface that represents a way to get differences between two objects of the same type.
 * An example usage is {@link uk.gov.hmcts.appregister.audit.listener.DataAuditLogger}
 */
public interface AuditDifferentiator {

    /**
     * Gets the differences between two objects of the same type
     * @param crudEnum The audit crud operation. This is used to determine how to treat the diff i.e. either by looking for
     *                 the specific annotation {@link uk.gov.hmcts.appregister.audit.listener.diff.Audit}, or looking for its
     *                 absence (in which case the whole object is diffed)
     * @param oldObj The original object
     * @param newObj The new object
     * @return The differences
     */
    List<Difference> diff(CRUDEnum crudEnum, Keyable oldObj, Keyable newObj);

    /**
     * Gets a differences against nothing which effectively means get all field changes are differences. Can be
     * used when creating or deleting where we dont have two objects to diff.
     * @return All field differences
     * @param crudEnum The audit crud operation. This is used to determine how to treat the diff i.e. either by looking for
     * the specific annotation {@link uk.gov.hmcts.appregister.audit.listener.diff.Audit}, or looking for its
     * absence (in which case the whole object is diffed)
     * @return The differences
     */
    List<Difference> diff(CRUDEnum crudEnum, Keyable newObj);

    /**
     * does diff recurse into nested objects
     * @return true if it does, false otherwise
     */
    boolean doesRecurseComplexObjects();

    /**
     * does diff recurse into nested objects
     * @return true if it does, false otherwise
     */
    boolean doesRecurseCollectionObjects();

}
