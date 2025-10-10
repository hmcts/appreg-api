package uk.gov.hmcts.appregister.common.entity.base;

import java.time.OffsetDateTime;

/**
 * This interface should be implemented by entities that need to support the legacy information that
 * is unmanged.
 */
public interface UnmanagedChangeable {
    /**
     * Gets the OID + TID of the user who made the last change.
     *
     * @return Changed by user number
     */
    Long getChangedBy();

    /**
     * Gets the date and time when the last change was made.
     *
     * @return Changed date and time
     */
    OffsetDateTime getChangedDate();
}
