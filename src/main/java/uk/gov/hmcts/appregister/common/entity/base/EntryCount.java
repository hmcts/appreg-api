package uk.gov.hmcts.appregister.common.entity.base;

import java.util.UUID;

/**
 * Projection interface representing the result of an aggregate query that counts
 * the number of {@code ApplicationListEntry} entities associated with each
 * {@code ApplicationList}.
 *
 * <p>This interface is typically used as a Spring Data JPA projection, where the
 * query returns a pair of values: the UUID of the {@code ApplicationList} and the
 * total count of its related entries. The projection allows the results to be
 * mapped directly into an interface without requiring a full entity fetch.</p>
 *
 */
public interface EntryCount {

    /**
     * Returns the unique identifier of the {@code ApplicationList} to which the
     * counted entries belong.
     *
     * @return the UUID of the parent {@code ApplicationList}
     */
    UUID getPrimaryKey();

    /**
     * Returns the number of {@code ApplicationListEntry} entities associated with
     * the {@code ApplicationList} identified by {@link #getPrimaryKey()}}.
     *
     * @return the count of entries, or {@code null} if no entries exist
     */
    Long getCount();
}
