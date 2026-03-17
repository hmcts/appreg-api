package uk.gov.hmcts.appregister.common.audit.listener.diff;

import java.util.List;
import uk.gov.hmcts.appregister.common.audit.listener.DataAuditLogger;
import uk.gov.hmcts.appregister.common.entity.base.Keyable;
import uk.gov.hmcts.appregister.common.enumeration.CrudEnum;

/**
 * An interface that represents a way to get auditable data for an object. An example usage is
 * {@link DataAuditLogger}.
 */
public interface Auditor {

    /**
     * Gets the auditable data for a given object.
     *
     * @param crudEnum The audit crud operation. This is used to determine how to treat the diff
     *     i.e. either by looking for the specific annotation {@link Audit}, or looking for its
     *     absence (in which case the whole object is diffed)
     * @param keyable The keyable object to extract the auditable data from
     * @return The auditable data
     */
    List<AuditableData> extractAuditData(CrudEnum crudEnum, Keyable keyable);

    /**
     * does audit recurse into nested objects.
     *
     * @return true if it does, false otherwise
     */
    boolean doesRecurseComplexObjects();
}
