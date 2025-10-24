package uk.gov.hmcts.appregister.audit.listener.diff;

import uk.gov.hmcts.appregister.common.entity.base.Keyable;

import java.util.List;

/**
 * An intefaces that represents a way to get differences between two objects of the same type.
 */
public interface AuditDifferentiator {

    /**
     * Gets the differences between two objects of the same type
     * @param t The original object
     * @param t1 The new object
     * @return The differences
     */
    List<Difference> diff(Keyable t, Keyable t1);

    /**
     * Gets a differences against nothing which effectively means get all field changes as differences
     * @return All field differences
     */
    List<Difference> diff(Keyable t1);
}
