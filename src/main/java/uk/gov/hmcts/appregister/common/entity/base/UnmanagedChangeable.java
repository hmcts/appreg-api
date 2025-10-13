package uk.gov.hmcts.appregister.common.entity.base;

import java.time.OffsetDateTime;

/**
 * This interface should be implemented by entities that need to support the legacy information that
 * is unmanaged.
 */
public interface UnmanagedChangeable extends UnmanagedEntity {
    /**
     * The change by id.
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
