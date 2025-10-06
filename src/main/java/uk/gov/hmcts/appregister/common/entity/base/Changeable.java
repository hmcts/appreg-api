package uk.gov.hmcts.appregister.common.entity.base;

import java.time.OffsetDateTime;

/**
 * This interface should be implemented by entities that need to track changes.
 */
public interface Changeable {

    /**
     * Gets the OID + TID of the user who made the last change.
     *
     * @return Changed by user number
     */
    String getChangedBy();

    /**
     * Gets the date and time when the last change was made.
     *
     * @return Changed date and time
     */
    OffsetDateTime getChangedDate();

    /**
     * Sets the OID + TID of the user who made the last change.
     *
     * @param changedBy Changed by user number
     */
    void setChangedBy(String changedBy);

    /**
     * Sets the date and time when the last change was made.
     *
     * @param changedDate Changed date and time
     */
    void setChangedDate(OffsetDateTime changedDate);
}
