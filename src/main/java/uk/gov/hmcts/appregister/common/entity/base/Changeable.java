package uk.gov.hmcts.appregister.common.entity.base;

import jakarta.persistence.EntityListeners;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** This interface should be implemented by entities that need to track changes. */
@EntityListeners(PreCreateUpdateEntityListener.class)
public interface Changeable {

    /**
     * Gets the ID of the user who made the last change.
     *
     * @return Changed by user number
     */
    BigDecimal getChangedBy();

    /**
     * Gets the date and time when the last change was made.
     *
     * @return Changed date and time
     */
    OffsetDateTime getChangedDate();

    /**
     * Sets the ID of the user who made the last change.
     *
     * @param changedBy Changed by user number
     */
    void setChangedBy(BigDecimal changedBy);

    /**
     * Sets the date and time when the last change was made.
     *
     * @param changedDate Changed date and time
     */
    void setChangedDate(OffsetDateTime changedDate);
}
