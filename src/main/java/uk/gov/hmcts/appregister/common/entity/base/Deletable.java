package uk.gov.hmcts.appregister.common.entity.base;

import java.time.OffsetDateTime;
import uk.gov.hmcts.appregister.common.enumeration.YesOrNo;

/**
 * An interface that describes the standard data that exists on an entity to represent its
 * deleteness.
 */
public interface Deletable {

    /**
     * Gets the OID + TID of the user who made the last delete.
     *
     * @return Delete by user number
     */
    String getDeletedBy();

    /**
     * Gets the date and time when the delete was made.
     *
     * @return Delete date and time
     */
    OffsetDateTime getDeletedDate();

    /**
     * Sets the OID + TID of the user who made the delete.
     *
     * @param deleteBy Changed by user number
     */
    void setDeletedBy(String deleteBy);

    /**
     * Sets the date and time when the delete was made.
     *
     * @param deleteBy Delete date and time
     */
    void setDeletedDate(OffsetDateTime deleteBy);

    /**
     * Is this record deleted.
     *
     * @return The enum representing true or false
     */
    YesOrNo getDeleted();

    /**
     * Sets the record as deleted.
     *
     * @param deleted the enum representing true or false
     */
    void setDeleted(YesOrNo deleted);

    /**
     * Sets the record as deleted.
     *
     * @param deleted A boolean
     */
    default void setDeleted(boolean deleted) {
        setDeleted(deleted ? YesOrNo.YES : YesOrNo.NO);
    }

    /**
     * Is this record deleted.
     *
     * @return A boolean
     */
    default boolean isDeleted() {
        return getDeleted() == YesOrNo.YES;
    }
}
